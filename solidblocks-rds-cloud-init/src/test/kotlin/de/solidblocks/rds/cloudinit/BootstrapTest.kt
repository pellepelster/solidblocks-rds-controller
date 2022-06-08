package de.solidblocks.rds.cloudinit

import mu.KotlinLogging
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.shaded.com.google.common.io.Files

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

@Disabled
class RdsAgentIntegrationTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testAgentUpdate() {

        val tempDir = Files.createTempDir()

        val docker = KGenericContainer("docker").apply {

        }

        docker.start()
    }
}
