package de.solidblocks.rds.controller.model.entities

import de.solidblocks.rds.controller.model.CloudConfigValue
import java.util.*

class RdsConfigurationId(id: UUID) : IdType(id)

data class RdsConfigurationEntity(
    val id: RdsConfigurationId,
    val rdsInstance: RdsInstanceId,
    val configValues: List<CloudConfigValue>
)
