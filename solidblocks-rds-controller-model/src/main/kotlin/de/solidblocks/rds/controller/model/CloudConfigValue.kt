package de.solidblocks.rds.controller.model

fun List<CloudConfigValue>.byName(name: String) = this.firstOrNull { it.name == name }?.value

data class CloudConfigValue(val name: String, val value: String?, val version: Int = 0)
