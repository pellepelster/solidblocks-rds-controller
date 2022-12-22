package de.solidblocks.rds.controller.model.status

import java.util.*

class StatusManager(val repository: StatusRepository) {

    fun update(id: UUID, status: Status) {
        repository.update(id, status)
    }

    fun latest(id: UUID) = repository.latest(id).let {
        if (it?.status == null) {
            return@let Status.UNKNOWN
        }

        Status.valueOf(it.status!!)
    }
}
