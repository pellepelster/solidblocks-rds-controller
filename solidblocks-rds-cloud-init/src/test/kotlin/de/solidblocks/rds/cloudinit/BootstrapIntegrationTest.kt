package de.solidblocks.rds.cloudinit

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import mu.KotlinLogging
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.shaded.com.google.common.io.Files
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration.ofSeconds
import java.util.*
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
    @Disabled
    fun testCloudInitBootstrap() {
        val tempDir = Files.createTempDir()

        val fileContent = UUID.randomUUID().toString()
        val cloudInitFile = tempDir.resolve("solidblocks-rds-cloud-init.sh")

        cloudInitFile.writeText(
            CloudInitTemplates.solidblocksRdsCloudInit(
                "version1",
                "/dev/data1",
                "/dev/backup1",
                "host1",
                "ca-public-key",
                "server-private-key",
                "server-public-key",
                "solidblocks-rds-postgresql-agent"
            )
        )

        java.nio.file.Files.setPosixFilePermissions(
            cloudInitFile.toPath(),
            PosixFilePermissions.fromString("r-xr-xr-x")
        )

        val compose =
            KDockerComposeContainer(File("src/test/resources/rds-cloud-init/docker-compose.yml")).withBuild(true)
                .withLogConsumer("test", Slf4jLogConsumer(logger)).withLogConsumer("wiremock", Slf4jLogConsumer(logger))
                .withExposedService("wiremock", 8080).waitingFor("wiremock", Wait.forListeningPort())
                .withEnv("CLOUD_INIT_FILE", cloudInitFile.absolutePath)
                .withEnv("REPOSITORY_BASE_ADDRESS", "http://wiremock:8080")
        compose.start()

        val wireMock = WireMock("localhost", compose.getServicePort("wiremock", 8080))
        wireMock.register(
            get("/solidblocks-rds-controller/solidblocks-rds-cloud-init/version1/solidblocks-rds-cloud-init-version1-assets.jar")
                .willReturn(
                    ok().withBody(createBootstrapJar(listOf("file1.txt" to fileContent)))
                )
        )

        await().atMost(ofSeconds(120)).until {
            val result =
                compose.getContainerByServiceName("test_1").get().execInContainer("cat", "/solidblocks/file1.txt")
            result.stdout.contains(fileContent)
        }

        wireMock.verifyThat(
            getRequestedFor(
                urlEqualTo("/solidblocks-rds-controller/solidblocks-rds-cloud-init/version1/solidblocks-rds-cloud-init-version1-assets.jar")
            )
        )
    }
}
