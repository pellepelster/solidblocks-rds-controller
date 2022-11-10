package de.solidblocks.rds.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ClientAuth
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.core.net.PemTrustOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

class AgentHttpServer(
    port: Int = 8080,
    caTrustCertificateRaw: String,
    privateKeyRaw: String,
    publicKeyRaw: String
) {

    private val jackson = jacksonObjectMapper()

    private val logger = KotlinLogging.logger {}

    private val shutdown = CountDownLatch(1)

    private var vertx: Vertx = Vertx.vertx()

    private var server: HttpServer

    private var router: Router

    init {
        val privateKey = Buffer.buffer(
            privateKeyRaw.replace(
                "BEGIN EC PRIVATE KEY",
                "BEGIN PRIVATE KEY"
            ).replace("END EC PRIVATE KEY", "END PRIVATE KEY")
        )
        val certificate: Buffer = Buffer.buffer(publicKeyRaw)
        val pemOptions = PemKeyCertOptions().setKeyValue(privateKey).setCertValue(certificate)

        val options = HttpServerOptions()
            .setSsl(true)
            .setClientAuth(ClientAuth.REQUIRED)
            .setPemTrustOptions(
                PemTrustOptions().addCertValue(Buffer.buffer(caTrustCertificateRaw))
            )
            .setPemKeyCertOptions(pemOptions)

        router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        server = vertx.createHttpServer(options)
        val completableFuture = CompletableFuture<Boolean>()

        server.exceptionHandler {
            logger.error(it) {
                "unhandled error"
            }
        }
        server.invalidRequestHandler {
            logger.warn {
                "invalid request '${it.path()}'"
            }
        }

        logger.info { "starting agent http api on port $port" }
        server.requestHandler(router).listen(port) {
            if (it.succeeded()) {
                logger.info { "agent http api has started on port ${server.actualPort()}" }
                completableFuture.complete(true)
            } else {
                logger.error(it.cause()) { "starting agent http api has failed" }
                completableFuture.complete(false)
            }
        }

        completableFuture.join()
    }

    fun waitForShutdownAndExit() {
        waitForShutdown()
        exitProcess(7)
    }

    fun waitForShutdown() {
        shutdown.await()
        server.close().result()
    }

    fun registerRoute(path: String, callback: (Router) -> Unit) {
        val subRouter = Router.router(vertx)
        router.route(path).subRouter(subRouter)
        callback.invoke(subRouter)
    }

    fun shutdown() {
        shutdown.countDown()
    }
}
