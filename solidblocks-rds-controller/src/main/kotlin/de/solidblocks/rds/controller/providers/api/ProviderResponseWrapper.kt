package de.solidblocks.rds.controller.providers.api

import de.solidblocks.rds.controller.api.MessageResponse

data class ProviderResponseWrapper(val provider: ProviderResponse? = null, val messages: List<MessageResponse> = emptyList())
