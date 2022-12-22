package de.solidblocks.rds.postgresql.agent

import de.solidblocks.rds.agent.LinuxCommandExecutor
import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.agent.initWorldReadableTempDir
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.shared.dto.TriggerUpdateRequest
import de.solidblocks.rds.shared.dto.TriggerUpdateResponse
import de.solidblocks.rds.shared.dto.VersionResponse
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class AgentWrapperProcess(solidblocksDirectory: Path, solidblocksVersion: String, solidblocksBootstrapAddress: String) {
    private val logger = KotlinLogging.logger {}

    private val commandExecutor: LinuxCommandExecutor = LinuxCommandExecutor()
    private val stopThread = AtomicBoolean(false)
    private var thread: Thread = thread {
        while (!stopThread.get()) {
            val workingDir = System.getProperty("user.dir")
            val result = commandExecutor.executeCommand(
                printStream = true,
                environment = mapOf(
                    "SOLIDBLOCKS_DIR" to solidblocksDirectory.toString(),
                    "SOLIDBLOCKS_DATA_DIR" to initWorldReadableTempDir("data-dir").absolutePath,
                    "SOLIDBLOCKS_BACKUP_DIR" to initWorldReadableTempDir("backup-dir").absolutePath,
                    "SOLIDBLOCKS_AGENT" to "solidblocks-rds-postgresql-agent",
                    "SOLIDBLOCKS_VERSION" to solidblocksVersion,
                    "SOLIDBLOCKS_BOOTSTRAP_ADDRESS" to solidblocksBootstrapAddress
                ),
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

class RdsPostgresqlAgentIntegrationTest {

    private val logger = KotlinLogging.logger {}

    private var agentWrapperProcess: AgentWrapperProcess? = null

    val serverCa = Utils.generateCAKeyPAir()
    val serverKeyPair = Utils.createCertificate(serverCa.privateKey, serverCa.publicKey)

    val clientCa = Utils.generateCAKeyPAir()
    val clientKeyPair = Utils.createCertificate(clientCa.privateKey, clientCa.publicKey)

    private val blueVersion =
        RdsPostgresqlAgentIntegrationTest::class.java.getResource("/rds-postgresql-agent/bootstrap/artefacts/blue.version")
            .readText().trim()

    private val greenVersion =
        RdsPostgresqlAgentIntegrationTest::class.java.getResource("/rds-postgresql-agent/bootstrap/artefacts/green.version")
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
                    )
                )
                withExposedService("bootstrap", 80)
            }
        dockerEnvironment.start()

        val solidblocksDirectory = createSolidblocksDirectory(
            blueVersion, clientCa.publicKey, serverKeyPair.privateKey, serverKeyPair.publicKey, dockerEnvironment
        )

        agentWrapperProcess = AgentWrapperProcess(
            solidblocksDirectory,
            blueVersion,
            "http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}"
        )

        val client = MtlsHttpClient(
            "https://localhost:8080", serverCa.publicKey, clientKeyPair.privateKey, clientKeyPair.publicKey
        )

        await.ignoreExceptions().until {
            client.get<VersionResponse>("/v1/agent/version").isSuccessful
        }

        assertThat(client.get<VersionResponse>("/v1/agent/version").data!!.version).isEqualTo(blueVersion)
        assertThat(
            client.post<TriggerUpdateResponse>(
                "/v1/agent/trigger-update",
                TriggerUpdateRequest(greenVersion)
            ).isSuccessful
        ).isTrue

        await.ignoreExceptions().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertThat(client.get<VersionResponse>("/v1/agent/version").data!!.version).isEqualTo(greenVersion)
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
                    )
                )
                withExposedService("bootstrap", 80)
            }
        dockerEnvironment.start()

        val solidblocksDirectory = createSolidblocksDirectory(
            blueVersion, clientCa.publicKey, serverKeyPair.privateKey, serverKeyPair.publicKey, dockerEnvironment
        )

        agentWrapperProcess = AgentWrapperProcess(
            solidblocksDirectory,
            blueVersion,
            "http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}"
        )

        val client = MtlsHttpClient(
            "https://localhost:8080", serverCa.publicKey, clientKeyPair.privateKey, clientKeyPair.publicKey
        )

        await.ignoreExceptions().until {
            client.get<VersionResponse>("/v1/agent/version").isSuccessful
        }

        assertThat(client.get<VersionResponse>("/v1/agent/version").data!!.version).isEqualTo(blueVersion)
        assertThat(
            client.post<TriggerUpdateResponse>(
                "/v1/agent/trigger-update",
                TriggerUpdateRequest("invalid-version")
            ).isSuccessful
        ).isTrue

        await.ignoreExceptions().withPollDelay(Duration.ofSeconds(20)).atMost(Duration.ofSeconds(25)).untilAsserted {
            assertThat(client.get<VersionResponse>("/v1/agent/version").data!!.version).isEqualTo(blueVersion)
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
