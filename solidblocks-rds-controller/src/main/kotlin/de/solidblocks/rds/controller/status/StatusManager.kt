package de.solidblocks.rds.controller.status

import de.solidblocks.rds.controller.api.StatusResponse
import de.solidblocks.rds.controller.model.entities.IdType
import de.solidblocks.rds.controller.model.status.HealthStatus
import de.solidblocks.rds.controller.model.status.ProvisioningStatus
import de.solidblocks.rds.controller.model.status.StatusRepository
import mu.KotlinLogging
import java.util.*

class StatusManager(val repository: StatusRepository) {

    private val logger = KotlinLogging.logger {}

    fun update(id: UUID, status: HealthStatus) {
        repository.update(id, status)
    }

    fun update(id: UUID, provisioning: ProvisioningStatus) {
        repository.update(id, provisioning)
    }

    fun update(id: IdType, provisioning: ProvisioningStatus) {
        repository.update(id.id, provisioning)
    }

    fun latest(id: UUID) = repository.latest(id).let {
        val health = it?.statusHealth ?: HealthStatus.UNKNOWN.toString()

        val provisioning = it?.statusProvisioning ?: HealthStatus.UNKNOWN.toString()

        try {

            StatusResponse(HealthStatus.valueOf(health), ProvisioningStatus.valueOf(provisioning))
        } catch (e: Exception) {
            logger.error(e) { "failed to parse health '${health}' and/or provisioning '${provisioning}'" }
            StatusResponse(HealthStatus.UNKNOWN, ProvisioningStatus.UNKNOWN)
        }

    }
}
