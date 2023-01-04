package de.solidblocks.rds.controller.status

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
        if (it?.statusHealth == null) {
            return@let HealthStatus.UNKNOWN
        }

        try {
            HealthStatus.valueOf(it.statusHealth!!)
        } catch (e: Exception) {
            logger.error(e) { "failed to parse health '${it.statusHealth}'" }
            return@let HealthStatus.UNKNOWN
        }
    }
}
