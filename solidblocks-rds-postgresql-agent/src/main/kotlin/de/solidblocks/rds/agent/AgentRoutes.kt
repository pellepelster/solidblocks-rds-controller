package de.solidblocks.rds.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.rds.agent.TriggerShutdownRequest.TRIGGER_SHUTDOWN_PATH
import de.solidblocks.rds.agent.TriggerUpdateRequest.Companion.TRIGGER_UPDATE_PATH
import de.solidblocks.rds.shared.AGENT_BASE_PATH
import de.solidblocks.rds.shared.VersionResponse
import de.solidblocks.rds.shared.VersionResponse.Companion.VERSION_PATH
import de.solidblocks.rds.shared.solidblocksVersion
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.CountDownLatch

class AgentRoutes(vertx: Vertx, router: Router, val shutdown: CountDownLatch) {

    val jackson = jacksonObjectMapper()

    private val logger = KotlinLogging.logger {}

    init {
        val subRouter = Router.router(vertx)
        router.mountSubRouter(AGENT_BASE_PATH, subRouter)

        subRouter.get(VERSION_PATH).handler {
            it.response().setStatusCode(200).end(jackson.writeValueAsString(VersionResponse(solidblocksVersion())))
        }

        subRouter.post(TRIGGER_SHUTDOWN_PATH).handler {
            it.response().setStatusCode(200).end()
            shutdown.countDown()
        }

        subRouter.post(TRIGGER_UPDATE_PATH).handler {

            val request = jackson.readValue(it.bodyAsString, TriggerUpdateRequest::class.java)

            val workingDir = System.getProperty("user.dir")
            val file = File(workingDir, "update.version")

            logger.info { "writing update trigger for version '${request.updateVersion}' to '$file'" }
            file.writeText(request.updateVersion)
            it.response().setStatusCode(200).end(jackson.writeValueAsString(TriggerUpdateResponse(true)))
            shutdown.countDown()
        }
    }
}
