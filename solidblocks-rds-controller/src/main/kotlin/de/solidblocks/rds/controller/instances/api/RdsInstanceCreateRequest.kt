package de.solidblocks.rds.controller.instances.api

import java.util.*

data class RdsInstanceCreateRequest(val name: String, val provider: UUID, val username: String, val password: String)
