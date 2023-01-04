package de.solidblocks.rds.controller.providers.api

import de.solidblocks.rds.base.jsonRequest
import de.solidblocks.rds.base.jsonResponse
import de.solidblocks.rds.controller.api.ApiHttpServer
import de.solidblocks.rds.controller.api.GenericApiResponse
import de.solidblocks.rds.controller.model.entities.ProviderId
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.status.StatusManager
import io.vertx.ext.web.RoutingContext
import java.util.*

class ProvidersApi(
    apiHttpServer: ApiHttpServer,
    private val manager: ProvidersManager,
    private val statusManager: StatusManager
) {

    init {
        apiHttpServer.configureSubRouter("/api/v1/providers", configure = { router ->
            router.get("/:id").handler(this::get)
            router.get("/:id/status").handler(this::status)
            router.delete("/:id").handler(this::delete)
            router.post().handler(this::create)
            router.get().handler(this::list)
        })
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(ProviderCreateRequest::class.java)

        val validateResult = manager.validate(request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(ProviderResponseWrapper(messages = validateResult.messages), 422)
            return
        }

        val result = manager.create(request)

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            ProviderResponseWrapper(provider = result.data), 201
        )
    }

    fun list(rc: RoutingContext) {
        rc.jsonResponse(
            ProvidersResponseWrapper(
                manager.list()
            )
        )
    }

    fun delete(rc: RoutingContext) {
        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(ProviderResponseWrapper(), 400)
            return
        }

        val deleteResult = manager.delete(id)
        if (deleteResult.hasErrors()) {
            rc.jsonResponse(ProviderResponseWrapper(messages = deleteResult.messages), 400)
        } else {
            rc.jsonResponse(ProviderResponseWrapper(), 204)
        }
    }

    fun get(rc: RoutingContext) {
        val id = try {
            ProviderId(UUID.fromString(rc.pathParam("id")))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(ProviderResponseWrapper(), 400)
            return
        }

        val provider = manager.read(id)

        if (provider == null) {
            rc.jsonResponse(ProviderResponseWrapper(), 404)
            return
        }

        rc.jsonResponse(ProviderResponseWrapper(provider = provider))
    }

    fun status(rc: RoutingContext) {
        val id = try {
            ProviderId(UUID.fromString(rc.pathParam("id")))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(ProviderResponseWrapper(), 400)
            return
        }

        val status = statusManager.latest(id.id)

        rc.jsonResponse(ProviderStatusResponse(id.id, status))
    }
}
