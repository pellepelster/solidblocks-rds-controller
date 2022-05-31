package de.solidblocks.rds.controller.model

import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import java.util.UUID

data class ProviderEntity(
    val id: UUID,
    val name: String,
    val configValues: List<CloudConfigValue>

) {
    fun apiKey(): String? = this.configValues.byName(API_KEY)
    fun sshPublicKey(): String? = this.configValues.byName(SSH_PUBLIC_KEY)
    fun sshPrivateKey(): String? = this.configValues.byName(SSH_PRIVATE_KEY)
}
