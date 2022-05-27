package de.solidblocks.rds.controller.providers.api

import de.solidblocks.rds.controller.api.MessageResponse

class ProvidersResponseWrapper(val providers: List<ProviderResponse>, val messages: List<MessageResponse> = emptyList())
