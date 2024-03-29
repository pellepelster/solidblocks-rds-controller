package de.solidblocks.rds.controller.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.byName
import java.util.*

class ProviderId(id: UUID) : IdType(id)

/*
fun RoutingContext.toProviderId() {

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
*/

data class ProviderEntity(
    val id: ProviderId,
    val name: String,
    val controller: ControllerId,
    val configValues: List<CloudConfigValue>

) {
    @get:JsonIgnore
    val apiKey: String get() = this.configValues.byName(API_KEY)!!

    @get:JsonIgnore
    val sshPublicKey: String get() = this.configValues.byName(SSH_PUBLIC_KEY)!!

    @get:JsonIgnore
    val sshPrivateKey: String get() = this.configValues.byName(SSH_PRIVATE_KEY)!!
}
