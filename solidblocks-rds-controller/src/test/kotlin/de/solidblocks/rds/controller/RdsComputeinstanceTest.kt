package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.providers.HetznerApi
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "HCLOUD_TOKEN", matches = ".*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RdsComputeinstanceTest {

    private val logger = KotlinLogging.logger {}

    private val hetznerApi = HetznerApi(System.getenv("HCLOUD_TOKEN"))
    private val hetznerCloudAPI = HetznerCloudAPI(System.getenv("HCLOUD_TOKEN"))

    @BeforeAll
    fun beforeAll() {
        cleanTestbed()
    }

    @AfterAll
    fun afterAll() {
        cleanTestbed()
    }

    fun cleanTestbed() {
        assertThat(hetznerApi.deleteAllSSHKeys()).isTrue
        assertThat(hetznerApi.deleteAllVolumes()).isTrue

        hetznerCloudAPI.servers.servers.forEach {
            hetznerCloudAPI.deleteServer(it.id)
        }
    }

    @Test
    fun testCreateRdsInstance() {
        assertThat(hetznerApi.hasServer("server1")).isFalse
        assertThat(hetznerApi.ensureVolume("server1-volume1")).isTrue
        assertThat(hetznerApi.ensureSSHKey("server1-sshkey1", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIM30a1OCQaueS/4U0IKOXs0Z9cozuz+04lPDlZCf8nLS pelle@fry")).isTrue

        assertThat(hetznerApi.ensureServer("server1", "server1-volume1", "", "server1-sshkey1")).isTrue
        assertThat(hetznerApi.ensureServer("server1", "server1-volume1", "", "server1-sshkey1")).isTrue
        assertThat(hetznerApi.hasServer("server1")).isTrue
        assertThat(hetznerApi.deleteServer("server1")).isTrue
        assertThat(hetznerApi.hasServer("server1")).isFalse
    }
}
