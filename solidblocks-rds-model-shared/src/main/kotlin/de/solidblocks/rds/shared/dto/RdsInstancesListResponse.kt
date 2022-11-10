package de.solidblocks.rds.shared.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class RdsInstancesListResponse(val id: UUID)
