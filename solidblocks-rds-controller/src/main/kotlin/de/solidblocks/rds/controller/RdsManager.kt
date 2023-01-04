package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.configuration.RdsConfigurationManager
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.model.status.StatusManager
import org.jooq.DSLContext
import java.util.*

class RdsManager(
    val dsl: DSLContext,
    val rdsInstancesManager: RdsInstancesManager,
    val rdsConfigurationManager: RdsConfigurationManager,
    val statusManager: StatusManager
) {
    fun validate(request: RdsInstanceCreateRequest) = rdsInstancesManager.validate(request)

    fun create(request: RdsInstanceCreateRequest) = dsl.transactionResult { _ ->
        val result = rdsInstancesManager.create(request) ?: return@transactionResult null

        rdsConfigurationManager.create(result.id)

        CreationResult(
            result.let {
                RdsInstanceResponse(it.id.id, it.name, it.provider.id, statusManager.latest(it.id.id))
            }
        )
    }

    fun list() = rdsInstancesManager.list()

    fun delete(id: UUID) = rdsInstancesManager.delete(id)

    fun read(id: UUID) = rdsInstancesManager.read(id)
}
