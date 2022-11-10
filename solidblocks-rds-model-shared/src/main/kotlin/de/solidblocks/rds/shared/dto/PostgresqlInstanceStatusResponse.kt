package de.solidblocks.rds.shared.dto

import java.util.*

data class PostgresqlInstanceStatusResponse(
    val id: UUID,
    val running: Boolean,
    val healthy: Boolean,
)
