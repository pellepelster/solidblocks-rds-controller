package de.solidblocks.rds.controller.providers

import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.ProvidersRepository
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
import de.solidblocks.rds.controller.utils.ErrorCodes
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import java.util.UUID

class ProvidersManager(
    private val repository: ProvidersRepository,
    private val rdsInstancesManager: RdsInstancesManager
) {

    private val logger = KotlinLogging.logger {}

    fun apply(): Boolean {

        val providers = repository.list()

        if (providers.isEmpty()) {
            logger.info {
                "no provider configurations found, skipping apply"
            }

            return true
        }

        return providers.map {
            logger.info {
                "applying config for provider '${it.name}'"
            }

            if (it.apiKey() == null) {
                logger.error {
                    "provider '${it.name}' has no api key configured"
                }
                return@map false
            }

            val hetznerApi = HetznerApi(it.apiKey()!!)
            val response = hetznerApi.ensureSSHKey(it.name, it.sshPublicKey()!!)

            if (!response) {
                logger.error {
                    "creating ssh key failed for provider '${it.name}'"
                }

                return@map false
            }

            return@map rdsInstancesManager.apply(hetznerApi, it)
        }.all { it }
    }

    fun get(id: UUID) = repository.read(id)?.let {
        ProviderResponse(it.id, it.name)
    }

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        ProviderResponse(it.id, it.name)
    }

    fun validate(request: ProviderCreateRequest): ValidationResult {

        if (repository.exists(request.name)) {
            return ValidationResult.error(ProviderCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        try {
            val cloudApi = HetznerCloudAPI(request.apiKey)
            cloudApi.datacenters.datacenters
        } catch (e: Exception) {
            return ValidationResult.error(ProviderCreateRequest::apiKey, ErrorCodes.INVALID)
        }

        return ValidationResult(emptyList())
    }

    fun create(request: ProviderCreateRequest): CreationResult<ProviderResponse> {
        val sshKey = Utils.generateSshKey(request.name)

        val entity = repository.create(
            request.name,
            mapOf(
                API_KEY to request.apiKey,
                SSH_PUBLIC_KEY to sshKey.publicKey,
                SSH_PRIVATE_KEY to sshKey.privateKey
            )
        )

        return CreationResult(
            entity?.let {
                ProviderResponse(it.id, it.name)
            }
        )
    }
}
