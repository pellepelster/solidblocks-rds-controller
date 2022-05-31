package de.solidblocks.rds.controller.providers

import de.solidblocks.rds.controller.ErrorCodes
import de.solidblocks.rds.controller.Utils
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.ProvidersRepository
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.request.SSHKeyRequest
import mu.KotlinLogging
import java.util.UUID

class ProvidersManager(private val providersRepository: ProvidersRepository) {

    private val logger = KotlinLogging.logger {}

    fun apply() {

        val providers = providersRepository.list()

        if (providers.isEmpty()) {

            logger.info {
                "no providers found, skipping apply"
            }

            return
        }

        for (provider in providersRepository.list()) {
            logger.info {
                "applying config for provider '${provider.name}'"
            }

            val cloudApi = HetznerCloudAPI(provider.apiKey()!!)

            val sshKey = cloudApi.getSSHKeyByName(provider.name)

            if (sshKey.sshKeys.isEmpty()) {
                logger.warn {
                    "ssh key not found for provider '${provider.name}'"
                }

                val response = cloudApi.createSSHKey(SSHKeyRequest(provider.name, provider.sshPublicKey()))

                if (response.sshKey != null) {
                    logger.info {
                        "created ssh key with fingerprint '${response.sshKey.fingerprint}'"
                    }
                } else {
                    logger.error {
                        "creating ssh key failed for provider '${provider.name}'"
                    }
                }
            }
        }
    }

    fun get(id: UUID) = providersRepository.read(id)?.let {
        ProviderResponse(it.id, it.name)
    }

    fun delete(id: UUID) = providersRepository.delete(id)

    fun list() = providersRepository.list().map {
        ProviderResponse(it.id, it.name)
    }

    fun validate(request: ProviderCreateRequest): ValidationResult {

        if (providersRepository.exists(request.name)) {
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

        val entity = providersRepository.create(
            request.name, mapOf(
                API_KEY to request.apiKey, SSH_PUBLIC_KEY to sshKey.second, SSH_PRIVATE_KEY to sshKey.first
            )
        )

        return CreationResult(entity?.let {
            ProviderResponse(it.id, it.name)
        })
    }

}