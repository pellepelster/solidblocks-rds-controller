package de.solidblocks.rds.controller.model.status

import mu.KotlinLogging
import java.util.*

class StatusManager(val repository: StatusRepository) {

    private val logger = KotlinLogging.logger {}

    fun update(id: UUID, status: Status) {
        repository.update(id, status)
    }

    fun latest(id: UUID) = repository.latest(id).let {
        if (it?.status == null) {
            return@let Status.UNKNOWN
        }

        try {
            Status.valueOf(it.status!!)
        } catch (e: Exception) {
            logger.error(e) { "failed to parse status '${it.status}'" }
            return@let Status.UNKNOWN
        }
    }
}
