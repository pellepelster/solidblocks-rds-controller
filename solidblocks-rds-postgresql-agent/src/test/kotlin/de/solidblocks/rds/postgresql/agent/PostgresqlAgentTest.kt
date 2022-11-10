package de.solidblocks.rds.postgresql.agent

import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.agent.initWorldReadableTempDir
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.shared.dto.*
import de.solidblocks.rds.shared.solidblocksVersion
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresqlAgentTest {

    private val serverCa = Utils.generateCAKeyPAir()
    private val serverKeyPair = Utils.createCertificate(serverCa.privateKey, serverCa.publicKey)

    private val clientCa = Utils.generateCAKeyPAir()
    private val clientKeyPair = Utils.createCertificate(clientCa.privateKey, clientCa.publicKey)

    private lateinit var client: MtlsHttpClient

    @BeforeAll
    fun beforeAll() {

        val id = "agent-test-${UUID.randomUUID()}"

        val dataDir = initWorldReadableTempDir(id)
        val backupDir = initWorldReadableTempDir(id)

        thread {
            PostgresqlAgent(
                8080,
                clientCa.publicKey,
                serverKeyPair.privateKey,
                serverKeyPair.publicKey,
                dataDir.toPath(), backupDir.toPath()
            ).waitForShutdown()
        }

        client = MtlsHttpClient(
            "https://localhost:8080",
            serverCa.publicKey,
            clientKeyPair.privateKey,
            clientKeyPair.publicKey
        )

        await.until {
            try {
                client.getRaw("/").code > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    @Test
    fun testRdsInstances() {
        var listResponse = client.get<RdsInstancesListResponseWrapper>("/v1/rds-instances")

        assertThat(listResponse.code).isEqualTo(200)
        assertThat(listResponse.data!!.rdsInstances).hasSize(0)

        val createResponse = client.postRaw(
            "/v1/rds-instances",
            CreatePostgresqlInstanceRequest(UUID.randomUUID(), "database2", "user2", "password2")
        )
        assertThat(createResponse.code).isEqualTo(201)

        listResponse = client.get("/v1/rds-instances")
        assertThat(listResponse.data!!.rdsInstances).hasSize(1)

        await.until({
            client.get<PostgresqlInstanceStatusResponse>("/v1/rds-instances/${listResponse.data!!.rdsInstances[0].id}/status")
        }, {
            it.data != null && it.data!!.running && it.data!!.healthy
        })
    }

    @Test
    fun testGetVersion() {
        val response = client.get<VersionResponse>("/v1/agent/version")

        assertThat(response.code).isEqualTo(200)
        assertThat(response.data!!.version).isEqualTo(solidblocksVersion())
    }

    @Test
    fun testGetSystemDf() {
        val response = client.get<DfResponse>("/v1/system/df")

        assertThat(response.code).isEqualTo(200)
        assertThat(response.data!!.df).filteredOn { it.mounted_on == "/run" }.hasSize(1)
    }

    @Test
    fun testGetSystemMount() {
        val response = client.get<MountResponse>("/v1/system/mount")

        assertThat(response.code).isEqualTo(200)
        assertThat(response.data!!.mount).filteredOn { it.type == "sysfs" }.hasSize(1)
    }
}
