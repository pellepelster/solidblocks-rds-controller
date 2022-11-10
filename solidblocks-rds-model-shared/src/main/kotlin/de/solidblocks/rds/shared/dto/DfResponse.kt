package de.solidblocks.rds.shared.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DfResponse(val df: List<DfResponseItem>)
