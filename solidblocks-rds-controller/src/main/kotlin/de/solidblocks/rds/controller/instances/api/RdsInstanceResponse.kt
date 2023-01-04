package de.solidblocks.rds.controller.instances.api

import de.solidblocks.rds.controller.api.StatusResponse
import java.util.*

data class RdsInstanceResponse(
    val id: UUID,
    val name: String,
    val provider: UUID,
    val status: StatusResponse,
)
