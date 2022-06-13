package de.solidblocks.rds.cloudinit

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import de.solidblocks.rds.shared.SharedConstants
import mu.KotlinLogging
import org.awaitility.Awaitility.*
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.shaded.com.google.common.io.Files
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration.ofSeconds
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

class BootstrapIntegrationTest {

    private val logger = KotlinLogging.logger {}

    fun createBootstrapJar(files: List<Pair<String, String>>) = ByteArrayOutputStream().use { out ->
        ZipOutputStream(out).use { zip ->
            for (srcFile in files) {
                val zipEntry = ZipEntry(srcFile.first)
                zip.putNextEntry(zipEntry)
                zip.write(srcFile.second.toByteArray())
            }
        }

        out
    }.toByteArray()

    @Test
    fun testBootstrap() {
        val tempDir = Files.createTempDir()

        val fileContent = UUID.randomUUID().toString()

        val cloudInitFile = tempDir.resolve("solidblocks-rds-cloud-init.sh")

        CloudInitTemplates.solidblocksRdsCloudInit(cloudInitFile.toPath(), "version1", "/dev/data1", "host1")

        val compose =
            KDockerComposeContainer(File("src/test/resources/rds-cloud-init/docker-compose.yml")).withBuild(true)
                .withLogConsumer("test", Slf4jLogConsumer(logger)).withLogConsumer("wiremock", Slf4jLogConsumer(logger))
                .withExposedService("wiremock", 8080).waitingFor("wiremock", Wait.forListeningPort())
                .withEnv("CLOUD_INIT_FILE", cloudInitFile.absolutePath)
                .withEnv("REPOSITORY_BASE_ADDRESS", "http://wiremock:8080")
                .withEnv("GITHUB_USERNAME", SharedConstants.githubUsername)
                .withEnv("GITHUB_PAT", SharedConstants.githubPat)
        compose.start()

        val wireMock = WireMock("localhost", compose.getServicePort("wiremock", 8080))
        wireMock.register(
            get("/pellepelster/solidblocks-rds/solidblocks-rds/solidblocks-rds-cloud-init/version1/solidblocks-rds-cloud-init-version1-assets.jar").willReturn(
                ok().withBody(createBootstrapJar(listOf("file1.txt" to fileContent)))
            )
        )

        await().atMost(ofSeconds(30)).until {
            val result =
                compose.getContainerByServiceName("test_1").get().execInContainer("cat", "/solidblocks/file1.txt")
            result.stdout.contains(fileContent)
        }
    }
}
