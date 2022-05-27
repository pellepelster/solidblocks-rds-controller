package de.solidblocks.rds.controller.api.provider

import de.solidblocks.rds.controller.ErrorCodes
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.model.ProvidersRepository

class ProviderManager(private val providersRepository: ProvidersRepository) {

    fun list() = providersRepository.list().map {
        ProviderResponse(it.id, it.name)
    }

    fun validate(request: ProviderCreateRequest): ValidationResult {

        if (providersRepository.exists(request.name)) {
            return ValidationResult.error(ProviderCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        return ValidationResult(emptyList())
    }

    fun create(request: ProviderCreateRequest): CreationResult<ProviderResponse> {
        val entity = providersRepository.create(request.name)

        return CreationResult(entity?.let {
            ProviderResponse(it.id, it.name)
        })
    }

}