package de.solidblocks.rds.postgresql.agent

import de.solidblocks.rds.agent.BaseAgentApiClient
import de.solidblocks.rds.agent.LinuxCommandExecutor
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.shared.SharedConstants
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class AgentWrapperProcess(solidblocksDirectory: Path) {
    private val logger = KotlinLogging.logger {}

    val commandExecutor: LinuxCommandExecutor = LinuxCommandExecutor()
    val stopThread = AtomicBoolean(false)
    var thread: Thread = thread {
        while (!stopThread.get()) {
            val workingDir = System.getProperty("user.dir")
            val result = commandExecutor.executeCommand(
                printStream = true,
                environment = mapOf("SOLIDBLOCKS_DIR" to solidblocksDirectory.toString()),
                workingDir = File(workingDir),
                command = listOf("$workingDir/../solidblocks-rds-cloud-init/assets/bin/solidblocks-agent-wrapper.sh").toTypedArray()
            )

            logger.info { "service wrapper exited with ${result.code}" }
        }
    }

    fun stop() {
        stopThread.set(true)
        commandExecutor.kill()
        thread.join(10000)
    }
}

class RdsAgentIntegrationTest {

    private val logger = KotlinLogging.logger {}

    private var agentWrapperProcess: AgentWrapperProcess? = null

    val serverCa = Utils.generateCAKeyPAir()
    val serverKeyPair = Utils.createCertificate(serverCa.privateKey, serverCa.publicKey)

    val clientCa = Utils.generateCAKeyPAir()
    val clientKeyPair = Utils.createCertificate(clientCa.privateKey, clientCa.publicKey)

    private val blueVersion =
        RdsAgentIntegrationTest::class.java.getResource("/rds-postgresql-agent/bootstrap/artefacts/blue.version")
            .readText().trim()

    private val greenVersion =
        RdsAgentIntegrationTest::class.java.getResource("/rds-postgresql-agent/bootstrap/artefacts/green.version")
            .readText().trim()

    @Test
    fun testAgentUpdate() {
        logger.info { "blue version for integration test is '$blueVersion'" }
        logger.info { "green version for integration test is '$greenVersion'" }

        val dockerEnvironment =
            KDockerComposeContainer(File("src/test/resources/rds-postgresql-agent/docker-compose.yml")).apply {
                withBuild(true).withEnv(
                    mapOf(
                        "SOLIDBLOCKS_BLUE_VERSION" to blueVersion,
                        "SOLIDBLOCKS_GREEN_VERSION" to greenVersion,
                        "GITHUB_USERNAME" to SharedConstants.githubUsername
                    )
                )
                withExposedService("bootstrap", 80)
            }
        dockerEnvironment.start()

        val solidblocksDirectory = createSolidblocksDirectory(
            blueVersion, clientCa.publicKey, serverKeyPair.privateKey, serverKeyPair.publicKey, dockerEnvironment
        )

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = BaseAgentApiClient(
            "https://localhost:8080", serverCa.publicKey, clientKeyPair.privateKey, clientKeyPair.publicKey
        )

        await ignoreException (ConnectException::class) until {
            client.version() != null
        }

        assertThat(client.version()?.version).isEqualTo(blueVersion)
        assertThat(client.triggerUpdate(greenVersion)).isTrue

        await atMost (Duration.ofSeconds(15)) ignoreException (ConnectException::class) untilAsserted {
            assertThat(client.version()?.version).isEqualTo(greenVersion)
        }
    }

    @Test
    fun testAgentUpdateInvalidVersion() {
        logger.info { "blue version for integration test is '$blueVersion'" }
        logger.info { "green version for integration test is '$greenVersion'" }

        val dockerEnvironment =
            KDockerComposeContainer(File("src/test/resources/rds-postgresql-agent/docker-compose.yml")).apply {
                withBuild(true).withEnv(
                    mapOf(
                        "SOLIDBLOCKS_BLUE_VERSION" to blueVersion,
                        "SOLIDBLOCKS_GREEN_VERSION" to greenVersion,
                        "GITHUB_USERNAME" to SharedConstants.githubUsername
                    )
                )
                withExposedService("bootstrap", 80)
            }
        dockerEnvironment.start()

        val solidblocksDirectory = createSolidblocksDirectory(
            blueVersion, clientCa.publicKey, serverKeyPair.privateKey, serverKeyPair.publicKey, dockerEnvironment
        )

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = BaseAgentApiClient(
            "https://localhost:8080", serverCa.publicKey, clientKeyPair.privateKey, clientKeyPair.publicKey
        )

        await ignoreException (ConnectException::class) until {
            client.version() != null
        }

        assertThat(client.version()?.version).isEqualTo(blueVersion)
        assertThat(client.triggerUpdate("invalid-version")).isTrue

        await withPollDelay (Duration.ofSeconds(20)) atMost (Duration.ofSeconds(25)) ignoreException (ConnectException::class) untilAsserted {
            assertThat(client.version()?.version).isEqualTo(blueVersion)
        }
    }

    private fun createSolidblocksDirectory(
        solidblocksVersion: String,
        solidblocksClientCaPublicKey: String,
        solidblocksServerPrivateKey: String,
        solidblocksServerPublicKey: String,
        dockerEnvironment: KDockerComposeContainer,
    ): Path {

        val solidblocksDir = Files.createTempDirectory("rds-agent")

        val protectedDir = File(solidblocksDir.toFile(), "protected")
        protectedDir.mkdirs()

        val initialEnvironmentFile = File(protectedDir, "environment")
        initialEnvironmentFile.writeText(
            """
            """.trimIndent()
        )
        logger.info { "created initial environment file: '$initialEnvironmentFile'" }

        File(protectedDir, "solidblocks_client_ca_public_key.crt").writeText(solidblocksClientCaPublicKey)
        File(protectedDir, "solidblocks_server_private_key.key").writeText(solidblocksServerPrivateKey)
        File(protectedDir, "solidblocks_server_public_key.crt").writeText(solidblocksServerPublicKey)

        val instanceDir = File(solidblocksDir.toFile(), "instance")
        instanceDir.mkdirs()

        val downloadDir = File(solidblocksDir.toFile(), "download")
        downloadDir.mkdirs()

        val instanceEnvironmentFile = File(instanceDir, "environment")
        instanceEnvironmentFile.writeText(
            """
            SOLIDBLOCKS_VERSION=$solidblocksVersion
            GITHUB_USERNAME=${SharedConstants.githubUsername}
            SOLIDBLOCKS_BOOTSTRAP_ADDRESS=http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}
            """.trimIndent()
        )

        logger.info { "created environment instance file: '$instanceEnvironmentFile'" }

        val serviceDir = File(solidblocksDir.toFile(), "service")
        serviceDir.mkdirs()

        val serviceEnvironmentFile = File(serviceDir, "environment")
        serviceEnvironmentFile.writeText(
            """
            """.trimIndent()
        )
        logger.info { "created service instance file: '$serviceEnvironmentFile'" }

        return solidblocksDir
    }

    @AfterEach
    fun cleanUp() {
        agentWrapperProcess?.stop()
    }
}
