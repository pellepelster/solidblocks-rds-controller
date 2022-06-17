package de.solidblocks.rds.controller.providers.api

import de.solidblocks.rds.controller.model.providers.ProviderStatus
import java.util.*

data class ProviderResponse(val id: UUID, val name: String, val controller: UUID, val status: ProviderStatus)
