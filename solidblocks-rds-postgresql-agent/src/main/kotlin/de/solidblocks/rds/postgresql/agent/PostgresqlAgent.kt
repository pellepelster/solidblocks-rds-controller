package de.solidblocks.rds.postgresql.agent

import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.rds.agent.AgentHttpServer
import de.solidblocks.rds.base.jsonRequest
import de.solidblocks.rds.base.jsonResponse
import de.solidblocks.rds.shared.dto.*
import de.solidblocks.rds.shared.solidblocksVersion
import io.vertx.ext.web.Router
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class PostgresqlAgent(
    port: Int = 8080,
    caTrustCertificateRaw: String,
    privateKeyRaw: String,
    publicKeyRaw: String,
    dataDir: Path,
    backupDir: Path,
    defaultDirectoryPermissions: String = "rwxrwxrwx"
) {
    private val logger = KotlinLogging.logger {}

    private val jackson = jacksonObjectMapper()

    private val httpServer: AgentHttpServer

    private val rdsInstancesManager: RdsInstancesManager

    private val stopThread = AtomicBoolean(false)

    private var thread: Thread

    init {
        rdsInstancesManager = RdsInstancesManager(dataDir, backupDir, defaultDirectoryPermissions)

        thread = thread {
            while (!stopThread.get()) {
                rdsInstancesManager.ensureInstances()
                Thread.sleep(1000)
            }
        }

        httpServer = AgentHttpServer(port, caTrustCertificateRaw, privateKeyRaw, publicKeyRaw)

        httpServer.registerRoute("/v1/system/*") {
            registerDfHandler(it)
            registerMountHandler(it)
        }

        httpServer.registerRoute("/v1/agent/*") {
            registerVersionHandler(it)
            registerTriggerUpdate(it)
            registerTriggerShutdown(it)
        }

        httpServer.registerRoute("/v1/rds-instances/*") {
            registerGetHandler(it)
            registerListHandler(it)
            registerCreateHandler(it)
        }
    }

    fun stop() {
        stopThread.set(true)
        thread.join(10000)
    }

    fun waitForShutdownAndExit() {
        httpServer.waitForShutdownAndExit()
    }

    fun waitForShutdown() {
        httpServer.waitForShutdown()
    }

    private fun registerListHandler(router: Router) {
        router.get().handler {
            it.response().setStatusCode(200)
                .end(
                    jackson.writeValueAsString(
                        RdsInstancesListResponseWrapper(
                            rdsInstancesManager.list().map {
                                RdsInstancesListResponse(it.id)
                            }
                        )
                    )
                )
        }
    }

    private fun registerGetHandler(router: Router) {
        router.get("/:id/status").handler {
            val id = try {
                UUID.fromString(it.pathParam("id"))
            } catch (e: IllegalArgumentException) {
                it.jsonResponse(400)
                return@handler
            }

            val status = rdsInstancesManager.status(id)
            if (status == null) {
                it.response().setStatusCode(400).end(jackson.writeValueAsString(null))
            } else {
                it.response().setStatusCode(200).end(jackson.writeValueAsString(status))
            }
        }
    }

    private fun registerCreateHandler(router: Router) {
        router.post().handler {
            val request = it.jsonRequest(CreatePostgresqlInstanceRequest::class.java)

            if (rdsInstancesManager.create(request.id, request.name, request.username, request.password)) {
                it.response().setStatusCode(201).end()
            } else {
                it.response().setStatusCode(400).end()
            }
        }
    }

    private fun registerVersionHandler(router: Router) {
        router.get("/version").handler {
            it.response().setStatusCode(200).end(jackson.writeValueAsString(VersionResponse(solidblocksVersion())))
        }
    }

    private fun registerTriggerUpdate(router: Router) {
        router.post("/trigger-update").handler {

            val request = jackson.readValue(it.bodyAsString, TriggerUpdateRequest::class.java)

            val workingDir = System.getProperty("user.dir")
            val file = File(workingDir, "update.version")

            logger.info { "writing update trigger for version '${request.updateVersion}' to '$file'" }
            file.writeText(request.updateVersion)
            it.response().setStatusCode(200).end(jackson.writeValueAsString(TriggerUpdateResponse(true)))
            httpServer.shutdown()
        }
    }

    private fun registerTriggerShutdown(router: Router) {
        router.post("/trigger-shutdown").handler {
            it.response().setStatusCode(200).end()
            httpServer.shutdown()
        }
    }

    private fun registerDfHandler(router: Router) {
        router.get("/df").handler {
            val pb = ProcessBuilder("/bin/sh", "-c", "df | jc --df")
            val process = pb.start()

            if (!process.waitFor(5000, TimeUnit.SECONDS)) {
                it.response().setStatusCode(400).end()
                return@handler
            }

            val stdout = process.inputReader().readText()
            val result = jackson.createObjectNode()
            result.putRawValue("df", RawValue(stdout))

            it.response().setStatusCode(200).end(jackson.writeValueAsString(result))
        }
    }

    private fun registerMountHandler(router: Router) {
        router.get("/mount").handler {
            val pb = ProcessBuilder("/bin/sh", "-c", "mount | jc --mount")
            val process = pb.start()

            if (!process.waitFor(5000, TimeUnit.SECONDS)) {
                it.response().setStatusCode(400).end()
                return@handler
            }

            val stdout = process.inputReader().readText()
            val result = jackson.createObjectNode()
            result.putRawValue("mount", RawValue(stdout))

            it.response().setStatusCode(200).end(jackson.writeValueAsString(result))
        }
    }
}
