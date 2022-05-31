package de.solidblocks.rds.controller.instances

import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.model.ProviderEntity
import de.solidblocks.rds.controller.model.RdsInstancesRepository
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.utils.Constants.data1VolumeName
import de.solidblocks.rds.controller.utils.Constants.serverName
import de.solidblocks.rds.controller.utils.Constants.sshKeyName
import de.solidblocks.rds.controller.utils.ErrorCodes
import mu.KotlinLogging
import java.util.*

class RdsInstancesManager(private val repository: RdsInstancesRepository) {

    private val logger = KotlinLogging.logger {}

    fun apply(hetznerApi: HetznerApi, provider: ProviderEntity): Boolean {
        val instances = repository.list(provider.id)

        if (!hetznerApi.cleanupServersNotInList(instances)) {
            logger.error {
                "cleaning up deleted servers failed"
            }

            return false
        }

        return instances.map {
            logger.info {
                "applying config for instance '${it.name}'"
            }

            hetznerApi.ensureVolume(data1VolumeName(it))
            hetznerApi.ensureServer(serverName(it), data1VolumeName(it), sshKeyName(provider))

            true
        }.all { it }
    }

    fun get(id: UUID) = repository.read(id)?.let {
        RdsInstanceResponse(it.id, it.name)
    }

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        RdsInstanceResponse(it.id, it.name)
    }

    fun validate(request: RdsInstanceCreateRequest): ValidationResult {

        if (repository.exists(request.name)) {
            return ValidationResult.error(RdsInstanceCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        return ValidationResult(emptyList())
    }

    fun create(request: RdsInstanceCreateRequest): CreationResult<RdsInstanceResponse> {

        val entity = repository.create(
            request.provider, request.name, mapOf()
        )

        return CreationResult(
            entity?.let {
                RdsInstanceResponse(it.id, it.name)
            }
        )
    }
}
