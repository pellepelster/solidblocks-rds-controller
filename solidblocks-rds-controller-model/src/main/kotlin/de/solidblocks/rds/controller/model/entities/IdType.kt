package de.solidblocks.rds.controller.model.entities

import java.util.*

sealed class IdType(val id: UUID) {
    override fun toString() = id.toString()
}
