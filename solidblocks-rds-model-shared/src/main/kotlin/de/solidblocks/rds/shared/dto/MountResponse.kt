package de.solidblocks.rds.shared.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MountResponse(val mount: List<MountResponseItem>)
