package de.solidblocks.rds.controller.providers.api

import de.solidblocks.rds.controller.api.StatusResponse
import de.solidblocks.rds.controller.model.status.HealthStatus
import java.util.*

data class ProviderStatusResponse(val id: UUID, val status: StatusResponse)
