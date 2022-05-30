package de.solidblocks.rds.controller.providers

import de.solidblocks.rds.controller.ErrorCodes
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.model.ProvidersRepository
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import java.util.UUID

class ProvidersManager(private val providersRepository: ProvidersRepository) {

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
        val entity = providersRepository.create(request.name, mapOf("apiKey" to request.apiKey))

        return CreationResult(entity?.let {
            ProviderResponse(it.id, it.name)
        })
    }

}