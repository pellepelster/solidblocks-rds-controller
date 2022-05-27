package de.solidblocks.rds.controller.api.provider

import de.solidblocks.rds.controller.api.ApiHttpServer
import de.solidblocks.rds.controller.api.GenericApiResponse
import de.solidblocks.rds.controller.api.jsonRequest
import de.solidblocks.rds.controller.api.jsonResponse
import io.vertx.ext.web.RoutingContext

class ProvidersApi(apiHttpServer: ApiHttpServer, val providerManager: ProviderManager) {

    init {
        apiHttpServer.configureSubRouter("/api/v1/providers", configure = { router ->
            router.get("/:id").handler(this::get)
            router.post().handler(this::create)
            router.get().handler(this::list)
        })
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(ProviderCreateRequest::class.java)

        val validateResult = providerManager.validate(request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(ProviderCreateResponse(messages = validateResult.messages), 422)
            return
        }

        val result = providerManager.create(request)

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            ProviderCreateResponse(provider = result.data), 201
        )
    }


    fun list(rc: RoutingContext) {
        rc.jsonResponse(
            ProvidersResponseWrapper(
                providerManager.list()
            )
        )
    }

    fun get(rc: RoutingContext) {

        /*
        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(CloudResponseWrapper(), 400)
            return
        }

        val cloud = cloudsManager.getCloud(rc.email(), id)

        if (cloud == null) {
            rc.jsonResponse(CloudResponseWrapper(), 404)
            return
        }

        val environments = cloudsManager.cloudEnvironments(rc.email(), id)
        rc.jsonResponse(CloudResponseWrapper(cloud.toResponse(environments.map { it.toResponse() })))

         */
    }
}
