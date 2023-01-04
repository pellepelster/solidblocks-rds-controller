package de.solidblocks.rds.controller

import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.cloudinit.CloudInitTemplates.Companion.solidblocksRdsCloudInit
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.docker.HealthChecks
import de.solidblocks.rds.shared.dto.VersionResponse
import de.solidblocks.rds.shared.solidblocksVersion
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.CreateServerRequest
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.InetSocketAddress
import java.time.Duration.ofSeconds

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
        assertThat(hetznerApi.ensureVolume("volume1", HetznerLabels())).isTrue
        assertThat(hetznerApi.ensureVolume("volume1", HetznerLabels())).isTrue
        assertThat(hetznerApi.hasVolume("volume1")).isTrue
    }

    @Test
    @Disabled
    fun testServerWorkflow() {

        val serverName = "server1"
        hetznerApi.ensureVolume("$serverName-volume1", HetznerLabels())
        hetznerApi.ensureSSHKey(
            "$serverName-sshkey1",
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBcyA+qHj+8AZx6GC02cl+K/oCBK/dbG7Wb1QFW+iHh3 pelle@pelle.io"
        )

        val serverCa = Utils.generateCAKeyPAir()
        val serverKeyPair = Utils.createCertificate(serverCa.privateKey, serverCa.publicKey)

        val clientCa = Utils.generateCAKeyPAir()
        val clientKeyPair = Utils.createCertificate(clientCa.privateKey, clientCa.publicKey)

        val cloudInit = solidblocksRdsCloudInit(
            solidblocksVersion(),
            "device1",
            "backup1",
            serverName,
            clientCa.publicKey,
            serverCa.privateKey,
            serverCa.publicKey,
            "solidblocks-rds-postgresql-agent"
        )

        val serverInfo = hetznerApi.ensureServer(
            serverName, listOf("$serverName-volume1"), cloudInit, "$serverName-sshkey1", HetznerLabels()
        )!!

        await().pollInterval(ofSeconds(5)).atMost(ofSeconds(120)).until {
            HealthChecks.checkPort(InetSocketAddress(serverInfo.ipAddress, serverInfo.agentPort))
        }

        val client = MtlsHttpClient(
            serverInfo.agentAddress,
            serverCa.publicKey,
            clientKeyPair.privateKey,
            clientKeyPair.publicKey
        )

        val version = client.get<VersionResponse>("/v1/agent/version")
        assertThat(version.code).isEqualTo(200)
        assertThat(version.data!!.version).startsWith(solidblocksVersion())
    }

    @Test
    fun testCreateAndDeleteServer() {
        assertThat(hetznerApi.hasServer("server1")).isFalse
        assertThat(hetznerApi.ensureVolume("server1-volume1", HetznerLabels())).isTrue
        assertThat(
            hetznerApi.ensureSSHKey(
                "server1-sshkey1",
                "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIM30a1OCQaueS/4U0IKOXs0Z9cozuz+04lPDlZCf8nLS pelle@fry"
            )
        ).isTrue

        assertThat(
            hetznerApi.ensureServer(
                "server1",
                listOf("server1-volume1"),
                "",
                "server1-sshkey1",
                HetznerLabels()
            )
        ).isNotNull
        assertThat(
            hetznerApi.ensureServer(
                "server1",
                listOf("server1-volume1"),
                "",
                "server1-sshkey1",
                HetznerLabels()
            )
        ).isNotNull
        assertThat(hetznerApi.hasServer("server1")).isTrue
        assertThat(hetznerApi.deleteServer("server1")).isTrue
        assertThat(hetznerApi.hasServer("server1")).isFalse
    }

    @Test
    fun testDoesNotDeleteUnmanagedServers() {

        val response = hetznerCloudAPI.createServer(
            CreateServerRequest.builder()
                .location("nbg1")
                .image("debian-11")
                .startAfterCreate(false)
                .serverType("cx11")
                .name("unamanged-server").build()
        )
        assertThat(hetznerApi.waitForAction(response.action)).isTrue

        assertThat(hetznerApi.hasServer("unamanged-server")).isFalse
        assertThat(hetznerApi.cleanupServersNotInList(emptyList())).isTrue
        assertThat(hetznerCloudAPI.servers.servers.any { it.name == "unamanged-server" }).isTrue
    }

    @Test
    fun testEnsureSSHKey() {
        assertThat(hetznerApi.hasSSHKey("sshkey1")).isFalse
        assertThat(
            hetznerApi.ensureSSHKey(
                "sshkey1",
                "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIA/3SavT/xyorfoGGkLjkWcxsJBf4J4KQf7wqu7B7G18 pelle@fry"
            )
        ).isTrue
        assertThat(
            hetznerApi.ensureSSHKey(
                "sshkey1",
                "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIA/3SavT/xyorfoGGkLjkWcxsJBf4J4KQf7wqu7B7G18 pelle@fry"
            )
        ).isTrue
        assertThat(hetznerApi.hasSSHKey("sshkey1")).isTrue
    }
}
