package de.solidblocks.rds.controller.instances.api

import de.solidblocks.rds.controller.api.ApiHttpServer
import de.solidblocks.rds.controller.api.GenericApiResponse
import de.solidblocks.rds.controller.api.jsonRequest
import de.solidblocks.rds.controller.api.jsonResponse
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import io.vertx.ext.web.RoutingContext
import java.util.*

class RdsInstancesApi(apiHttpServer: ApiHttpServer, val manager: RdsInstancesManager) {

    init {
        apiHttpServer.configureSubRouter("/api/v1/rds-instances", configure = { router ->
            router.get("/:id").handler(this::get)
            router.delete("/:id").handler(this::delete)
            router.post().handler(this::create)
            router.get().handler(this::list)
        })
    }

    fun create(rc: RoutingContext) {
        val request = rc.jsonRequest(RdsInstanceCreateRequest::class.java)

        val validateResult = manager.validate(request)

        if (validateResult.hasErrors()) {
            rc.jsonResponse(RdsInstanceResponseWrapper(messages = validateResult.messages), 422)
            return
        }

        val result = manager.create(request)

        if (result.data == null) {
            rc.jsonResponse(GenericApiResponse(), 500)
        }

        rc.jsonResponse(
            RdsInstanceResponseWrapper(rdsInstance = result.data), 201
        )
    }

    fun list(rc: RoutingContext) {
        rc.jsonResponse(
            RdsInstancesResponseWrapper(
                manager.list()
            )
        )
    }

    fun delete(rc: RoutingContext) {
        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(RdsInstanceResponseWrapper(), 400)
            return
        }

        if (manager.delete(id)) {
            rc.jsonResponse(RdsInstanceResponseWrapper(), 204)
        } else {
            rc.jsonResponse(RdsInstanceResponseWrapper(), 404)
        }
    }

    fun get(rc: RoutingContext) {
        val id = try {
            UUID.fromString(rc.pathParam("id"))
        } catch (e: IllegalArgumentException) {
            rc.jsonResponse(RdsInstanceResponseWrapper(), 400)
            return
        }

        val provider = manager.read(id)

        if (provider == null) {
            rc.jsonResponse(RdsInstanceResponseWrapper(), 404)
            return
        }

        rc.jsonResponse(RdsInstanceResponseWrapper(rdsInstance = provider))
    }
}
