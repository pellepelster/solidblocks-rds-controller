package de.solidblocks.rds.controller.api

import de.solidblocks.rds.controller.model.status.HealthStatus
import de.solidblocks.rds.controller.model.status.ProvisioningStatus

data class StatusResponse(val health: HealthStatus, val provisioning: ProvisioningStatus)