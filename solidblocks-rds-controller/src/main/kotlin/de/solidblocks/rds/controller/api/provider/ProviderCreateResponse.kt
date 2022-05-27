package de.solidblocks.rds.controller.api.provider

import de.solidblocks.rds.controller.api.MessageResponse

data class ProviderCreateResponse(val provider: ProviderResponse? = null, val messages: List<MessageResponse> = emptyList())
