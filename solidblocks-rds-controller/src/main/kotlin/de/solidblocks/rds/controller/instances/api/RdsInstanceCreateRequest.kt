package de.solidblocks.rds.controller.instances.api

import java.util.UUID

data class RdsInstanceCreateRequest(val name: String, val provider: UUID)
