package de.solidblocks.rds.controller.instances.api

import de.solidblocks.rds.controller.api.MessageResponse

class RdsInstancesResponseWrapper(val rdsInstances: List<RdsInstanceResponse>, val messages: List<MessageResponse> = emptyList())
