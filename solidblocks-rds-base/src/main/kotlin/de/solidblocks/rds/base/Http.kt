package de.solidblocks.rds.base

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.ext.web.RoutingContext

val jackson = jacksonObjectMapper()
fun RoutingContext.jsonResponse(response: Any, code: Int = 200) {
    this.response().setStatusCode(code).putHeader("Content-Type", "application/json")
        .end(jackson.writeValueAsString(response))
}

fun RoutingContext.jsonResponse(code: Int = 200) {
    this.response().setStatusCode(code).end()
}

fun <T> RoutingContext.jsonRequest(clazz: Class<T>): T {
    return jackson.readValue(this.body.bytes, clazz)
}
