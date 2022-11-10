package de.solidblocks.rds.shared.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MountResponseItem(
    val filesystem: String,
    val mount_point: String,
    val type: String,
    val options: List<String>,
)
