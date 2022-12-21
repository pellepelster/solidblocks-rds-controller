package de.solidblocks.rds.controller.model.controllers

import com.fasterxml.jackson.annotation.JsonIgnore
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.Constants.CA_CLIENT_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.CA_CLIENT_PUBLIC_KEY
import de.solidblocks.rds.controller.model.Constants.CA_SERVER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.CA_SERVER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.byName
import java.util.*

data class ControllerEntity(
    val id: UUID,
    val name: String,
    val configValues: List<CloudConfigValue>
) {

    @get:JsonIgnore
    val caServerPrivateKey: String get() = this.configValues.byName(CA_SERVER_PRIVATE_KEY)!!

    @get:JsonIgnore
    val caServerPublicKey: String get() = this.configValues.byName(CA_SERVER_PUBLIC_KEY)!!

    @get:JsonIgnore
    val caClientPrivateKey: String get() = this.configValues.byName(CA_CLIENT_PRIVATE_KEY)!!

    @get:JsonIgnore
    val caClientPublicKey: String get() = this.configValues.byName(CA_CLIENT_PUBLIC_KEY)!!
}
