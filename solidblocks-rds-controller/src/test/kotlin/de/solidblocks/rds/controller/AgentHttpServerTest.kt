package de.solidblocks.rds.controller

import de.solidblocks.rds.agent.AgentHttpServer
import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.controller.utils.Utils
import de.solidblocks.rds.shared.VersionResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentHttpServerTest {

    val serverCa = Utils.generateCAKeyPAir()
    val serverKeyPair = Utils.createCertificate(serverCa.privateKey, serverCa.publicKey)

    val clientCa = Utils.generateCAKeyPAir()
    val clientKeyPair = Utils.createCertificate(clientCa.privateKey, clientCa.publicKey)

    @BeforeAll
    fun beforeAll() {

        thread {
            AgentHttpServer(8080, clientCa.publicKey, serverKeyPair.privateKey, serverKeyPair.publicKey).waitForShutdown()
        }

        /*
        Awaitility.await().until {
            try {
                Socket("127.0.0.1", 8080).use {
                    it.getInputStream().read() > 0
                }
            } catch (e: Exception) {
                false
            }
        }*/
    }

    @AfterAll
    fun afterAll() {
    }

    @Test
    fun testGetVersion() {
        val client = MtlsHttpClient("https://localhost:8080", serverCa.publicKey, clientKeyPair.privateKey, clientKeyPair.publicKey)
        val version = client.get<VersionResponse>("/v1/agent/version")

        assertThat(version.code).isEqualTo(200)
        assertThat(version.data!!.version).startsWith("SNAPSHOT-")
    }
}
