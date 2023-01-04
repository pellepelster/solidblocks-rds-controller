package de.solidblocks.rds.controller.instances.api

import de.solidblocks.rds.controller.model.status.Status
import java.util.*

data class RdsInstanceResponse(
    val id: UUID,
    val name: String,
    val provider: UUID,
    val status: Status,
)
