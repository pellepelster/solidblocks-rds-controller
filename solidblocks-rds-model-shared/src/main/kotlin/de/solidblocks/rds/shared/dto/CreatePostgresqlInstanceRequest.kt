package de.solidblocks.rds.shared.dto

import java.util.UUID

data class CreatePostgresqlInstanceRequest(
    val id: UUID,
    val name: String,
    val username: String,
    val password: String
)
