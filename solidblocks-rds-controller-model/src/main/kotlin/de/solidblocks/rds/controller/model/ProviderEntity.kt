package de.solidblocks.rds.controller.model

import java.util.UUID

data class ProviderEntity(
    val id: UUID,
    val name: String,
    val configValues: List<CloudConfigValue>
)