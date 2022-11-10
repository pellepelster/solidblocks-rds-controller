package de.solidblocks.rds.shared.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DfResponseItem(
    val filesystem: String,
    val used: Int,
    val available: Int,
    val use_percent: Int,
    val mounted_on: String,
)
