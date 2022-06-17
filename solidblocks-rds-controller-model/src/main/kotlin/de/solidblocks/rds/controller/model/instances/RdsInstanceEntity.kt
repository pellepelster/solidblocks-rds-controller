package de.solidblocks.rds.controller.model.instances

import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.Constants.SERVER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SERVER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.byName
import java.util.UUID

data class RdsInstanceEntity(
    val id: UUID,
    val name: String,
    val provider: UUID,
    val status: RdsInstanceStatus,
    val configValues: List<CloudConfigValue>
) {
    fun serverPrivateKey(): String = this.configValues.byName(SERVER_PRIVATE_KEY)!!
    fun serverPublicKey(): String = this.configValues.byName(SERVER_PUBLIC_KEY)!!
}
