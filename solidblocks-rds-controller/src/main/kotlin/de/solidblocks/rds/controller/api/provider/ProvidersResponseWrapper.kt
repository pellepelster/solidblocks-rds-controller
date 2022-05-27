package de.solidblocks.rds.controller.api.provider

import de.solidblocks.rds.controller.api.MessageResponse

class ProvidersResponseWrapper(val providers: List<ProviderResponse>, val messages: List<MessageResponse> = emptyList())
