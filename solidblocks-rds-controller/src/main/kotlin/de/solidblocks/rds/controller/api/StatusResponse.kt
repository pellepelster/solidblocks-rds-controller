package de.solidblocks.rds.controller.api

import de.solidblocks.rds.controller.model.status.HealthStatus

data class StatusResponse(val health: HealthStatus)