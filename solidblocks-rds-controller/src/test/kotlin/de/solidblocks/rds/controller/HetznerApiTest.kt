package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.providers.HetznerApi
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.ServerRequest
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "HCLOUD_TOKEN", matches = ".*")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HetznerApiTest {

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
    fun testEnsureVolume() {
        assertThat(hetznerApi.hasVolume("volume1")).isFalse
        assertThat(hetznerApi.ensureVolume("volume1")).isTrue
        assertThat(hetznerApi.ensureVolume("volume1")).isTrue
        assertThat(hetznerApi.hasVolume("volume1")).isTrue
    }

    @Test
    fun testCreateAndDeleteServer() {
        assertThat(hetznerApi.hasServer("server1")).isFalse
        assertThat(hetznerApi.ensureVolume("server1-volume1")).isTrue
        assertThat(hetznerApi.ensureSSHKey("server1-sshkey1", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIM30a1OCQaueS/4U0IKOXs0Z9cozuz+04lPDlZCf8nLS pelle@fry")).isTrue

        assertThat(hetznerApi.ensureServer("server1", "server1-volume1", "server1-sshkey1")).isTrue
        assertThat(hetznerApi.ensureServer("server1", "server1-volume1", "server1-sshkey1")).isTrue
        assertThat(hetznerApi.hasServer("server1")).isTrue
        assertThat(hetznerApi.deleteServer("server1")).isTrue
        assertThat(hetznerApi.hasServer("server1")).isFalse
    }

    @Test
    fun testDoesNotDeleteUnmanagedServers() {

        val response = hetznerCloudAPI.createServer(
            ServerRequest.builder()
                .location("nbg1")
                .image("debian-10")
                .startAfterCreate(false)
                .serverType("cx11")
                .name("unamanged-server").build()
        )
        assertThat(hetznerApi.waitForServerAction(response.server, response.action)).isTrue

        assertThat(hetznerApi.hasServer("unamanged-server")).isFalse
        assertThat(hetznerApi.cleanupServersNotInList(emptyList())).isTrue
        assertThat(hetznerCloudAPI.servers.servers.any { it.name == "unamanged-server" }).isTrue
    }

    @Test
    fun testEnsureSSHKey() {
        assertThat(hetznerApi.hasSSHKey("sshkey1")).isFalse
        assertThat(hetznerApi.ensureSSHKey("sshkey1", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIA/3SavT/xyorfoGGkLjkWcxsJBf4J4KQf7wqu7B7G18 pelle@fry")).isTrue
        assertThat(hetznerApi.ensureSSHKey("sshkey1", "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIA/3SavT/xyorfoGGkLjkWcxsJBf4J4KQf7wqu7B7G18 pelle@fry")).isTrue
        assertThat(hetznerApi.hasSSHKey("sshkey1")).isTrue
    }
}
