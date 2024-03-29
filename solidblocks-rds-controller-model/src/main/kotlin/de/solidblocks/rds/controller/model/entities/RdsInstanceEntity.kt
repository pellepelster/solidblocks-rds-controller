package de.solidblocks.rds.controller.model.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.Constants.PASSWORD
import de.solidblocks.rds.controller.model.Constants.SERVER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SERVER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.Constants.USERNAME
import de.solidblocks.rds.controller.model.byName
import java.util.*

class RdsInstanceId(id: UUID) : IdType(id)

data class RdsInstanceEntity(
    val id: RdsInstanceId,
    val name: String,
    val provider: ProviderId,
    val configValues: List<CloudConfigValue>
) {
    @get:JsonIgnore
    val serverPrivateKey: String get() = this.configValues.byName(SERVER_PRIVATE_KEY)!!

    @get:JsonIgnore
    val serverPublicKey: String get() = this.configValues.byName(SERVER_PUBLIC_KEY)!!

    @get:JsonIgnore
    val username: String get() = this.configValues.byName(USERNAME)!!

    @get:JsonIgnore
    val password: String get() = this.configValues.byName(PASSWORD)!!
}
