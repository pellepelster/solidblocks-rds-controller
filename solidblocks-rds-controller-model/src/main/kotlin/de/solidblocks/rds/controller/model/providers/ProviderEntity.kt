package de.solidblocks.rds.controller.model.providers

import com.fasterxml.jackson.annotation.JsonIgnore
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.byName
import java.util.*

data class ProviderEntity(
    val id: UUID,
    val name: String,
    val controller: UUID,
    val configValues: List<CloudConfigValue>

) {
    @get:JsonIgnore
    val apiKey: String get() = this.configValues.byName(API_KEY)!!

    @get:JsonIgnore
    val sshPublicKey: String get() = this.configValues.byName(SSH_PUBLIC_KEY)!!

    @get:JsonIgnore
    val sshPrivateKey: String get() = this.configValues.byName(SSH_PRIVATE_KEY)!!
}
