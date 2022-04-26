package de.solidblocks.rds.postgres

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.command.LogContainerResultCallback
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.rds.postgres.Constants.SERVICE_ID_KEY
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


class DockerManager(
    private val id: String,
    private val dockerImage: String,
    private val ports: Set<Int> = emptySet(),
    private val healthCheck: (address: InetSocketAddress) -> Boolean,
    private val environment: Map<String, String> = emptyMap(),
    private val bindings: Map<Path, String> = emptyMap(),
    private val network: String? = null
) {
    private val logger = KotlinLogging.logger {}

    private val dockerClient: DockerClient

    private val retryConfig: RetryConfig.Builder<Boolean> =
        RetryConfig.custom<Boolean>().retryOnResult { it == false }.maxAttempts(20).waitDuration(Duration.ofSeconds(1))

    private val retry: Retry = Retry.of("healthcheck", retryConfig.build())

    private val stopLogCollection = AtomicBoolean(false)

    init {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

        dockerClient = DockerClientImpl.getInstance(
            config, ZerodepDockerHttpClient.Builder().dockerHost(URI.create("unix:///var/run/docker.sock")).build()
        )
    }

    fun existsImage(imageName: String): Boolean = try {
        dockerClient.inspectImageCmd(imageName).exec()
        true
    } catch (e: NotFoundException) {
        false
    }

    fun start(): Boolean {
        logger.info { "starting docker image '$dockerImage'" }

        if (!existsImage(dockerImage)) {
            try {
                val pullResult = dockerClient.pullImageCmd(dockerImage).start().awaitCompletion(5, TimeUnit.MINUTES)
                if (!pullResult) {
                    logger.error { "failed to pull docker image '$dockerImage'" }
                    return false
                }
            } catch (e: NotFoundException) {
                logger.error(e) { "failed to pull docker image '$dockerImage'" }
                return false
            }
        }

        val hostConfig = HostConfig.newHostConfig()
            .withPortBindings(ports.map { PortBinding(Ports.Binding.empty(), ExposedPort(it)) }).withAutoRemove(true)
            .withBinds(createBindings())

        if (network != null) {
            hostConfig.withNetworkMode(network)
        }

        val result = dockerClient.createContainerCmd(dockerImage).withExposedPorts(ports.map { ExposedPort(it) })
            .withEnv(environment.map { "${it.key}=${it.value}" }).withLabels(mapOf(SERVICE_ID_KEY to id))
            .withHostConfig(hostConfig).exec()

        dockerClient.startContainerCmd(result.id).exec()

        val thread = thread(start = true) {

            while (!stopLogCollection.get()) {
                try {
                    val logContainerCmd = dockerClient.logContainerCmd(result.id)
                    logContainerCmd.withStdOut(true).withStdErr(true)
                    logContainerCmd.withFollowStream(true)
                    logContainerCmd.withSince(Instant.now().nano)
                    logContainerCmd.withTimestamps(true)

                    logContainerCmd.exec(object : LogContainerResultCallback() {
                        override fun onNext(item: Frame) {
                            logger.info { item.toString() }
                        }
                    }).awaitCompletion()
                } catch (e: NotFoundException) {
                    stopLogCollection.set(true)
                }
            }
        }

        return waitForRunning() && waitForHealthy()
    }

    private fun createBindings(): List<Bind> {
        return bindings.map { Bind(it.key.toString(), Volume(it.value)) }
    }

    fun mappedPorts(port: Int) = serviceContainers().flatMap {
        val inspect = dockerClient.inspectContainerCmd(it.id).exec()
        inspect.networkSettings.ports.bindings.entries
    }.filter { it.key.port == port }.map { it.value.map { it.hostPortSpec.toInt() }.firstOrNull() }.firstOrNull()

    private fun waitForRunning(): Boolean {

        val result = retry.executeCallable {
            logger.info { "waiting for containers for service '$id'" }
            isRunning()
        }

        if (!result) {
            logger.error { "service '$id' not running" }
        } else {
            logger.info { "service '$id' running" }
        }

        return result
    }

    private fun waitForHealthy(): Boolean {
        val result = retry.executeCallable {
            logger.info { "waiting for service '$id' to become healthy" }
            isHealthy()
        }

        if (!result) {
            logger.error { "service '$id' not healthy" }
        } else {
            logger.info { "service '$id' healthy" }
        }

        return result
    }

    private fun serviceContainers() = dockerClient.listContainersCmd().exec().filter { it.labels[SERVICE_ID_KEY] == id }

    fun stop(): Boolean {
        serviceContainers().forEach {
            logger.info { "stopping container '${it.id}' for service '$id'" }
            dockerClient.stopContainerCmd(it.id).exec()
        }

        stopLogCollection.set(true)

        return true
    }

    fun isRunning(): Boolean = serviceContainers().isNotEmpty()

    fun isHealthy(): Boolean {
        val total = 5
        return ports.mapNotNull {
            mappedPorts(it)?.let { port ->
                (0 until total).map { i ->
                    Thread.sleep(1000)
                    logger.info { "running healtcheck ${i} out of ${total}" }
                    healthCheck.invoke(InetSocketAddress("localhost", port))
                }.all { it }
            }
        }.all { it }
    }
}
