package de.solidblocks.rds.controller.model.controllers

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

    fun caServerPrivateKey(): String = this.configValues.byName(CA_SERVER_PRIVATE_KEY)!!
    fun caServerPublicKey(): String = this.configValues.byName(CA_SERVER_PUBLIC_KEY)!!

    fun caClientPrivateKey(): String = this.configValues.byName(CA_CLIENT_PRIVATE_KEY)!!
    fun caClientPublicKey(): String = this.configValues.byName(CA_CLIENT_PUBLIC_KEY)!!
}
