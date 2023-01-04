package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.StatusResponse
import de.solidblocks.rds.controller.configuration.RdsConfigurationManager
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.status.StatusManager
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
                RdsInstanceResponse(it.id.id, it.name, it.provider.id, StatusResponse(statusManager.latest(it.id.id)))
            }
        )
    }

    fun list() = rdsInstancesManager.list()

    fun delete(id: UUID) = dsl.transactionResult { _ ->
        rdsConfigurationManager.deleteByInstance(id)
        rdsInstancesManager.delete(id)

        true
    }

    fun read(id: UUID) = rdsInstancesManager.read(id)
}
