package de.solidblocks.rds.controller

import de.solidblocks.rds.agent.AgentHttpServer
import de.solidblocks.rds.controller.utils.Utils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentHttpServerTest {

    @BeforeAll
    fun beforeAll() {
        val server = Utils.generateX509Certificate()
        val client = Utils.generateX509Certificate()

        thread {
            AgentHttpServer(8080, client.publicKey, server.privateKey, server.publicKey).waitForShutdown()
        }
    }

    @AfterAll
    fun afterAll() {
    }

    @Test
    fun testShutdown() {
        Thread.sleep(100000)
        this.toString()
    }
}
