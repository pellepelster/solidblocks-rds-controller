package de.solidblocks.rds.controller.instances.api

import de.solidblocks.rds.controller.api.MessageResponse

class RdsInstanceResponseWrapper(val rdsInstance: RdsInstanceResponse? = null, val messages: List<MessageResponse> = emptyList())
