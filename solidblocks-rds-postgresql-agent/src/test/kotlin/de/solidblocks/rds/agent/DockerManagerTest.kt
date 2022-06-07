package de.solidblocks.rds.agent

import de.solidblocks.rds.docker.DockerManager
import de.solidblocks.rds.docker.HealthChecks
import de.solidblocks.rds.docker.defaultHttpClient
import mu.KotlinLogging
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class DockerManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testStartStopService() {

        val id = "docker-manager-${UUID.randomUUID()}"

        val webDir = initWorldReadableTempDir(id)

        File(webDir, "test.txt").writeText("Hello World!")

        val dockerManager = DockerManager(
            id = id,
            dockerImage = "halverneus/static-file-server",
            ports = setOf(8080),
            bindings = mapOf(webDir.toPath() to "/web"),
            healthCheck = HealthChecks::checkPort
        )

        assertThat(dockerManager.start()).isTrue
        assertThat(dockerManager.isRunning()).isTrue
        assertThat(dockerManager.isHealthy()).isTrue

        val httpClient = defaultHttpClient()
        val request = Request.Builder()
            .url("http://localhost:${dockerManager.mappedPorts(8080)}/test.txt")
            .build()
        assertThat(httpClient.newCall(request).execute().body!!.string()).isEqualTo("Hello World!")

        dockerManager.stop()

        assertThat(dockerManager.isRunning()).isFalse
    }
}
