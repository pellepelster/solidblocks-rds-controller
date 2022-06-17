package de.solidblocks.rds.controller.utils

import de.solidblocks.rds.shared.solidblocksVersion
import java.lang.RuntimeException
import java.security.MessageDigest

class HetznerLabels(hetznerLabels: Map<String, String> = HashMap()) {

    private val labels: HashMap<String, String> = HashMap()

    private val MAX_VALUE_LENGTH = 62

    private val MAX_TOTAL_VALUE_LENGTH = MAX_VALUE_LENGTH * 2

    init {

        mapOf(
            Constants.managedByLabel to "true",
            Constants.versionLabel to solidblocksVersion(),
            Constants.cloudInitChecksumLabel to solidblocksVersion()
        ).forEach { (key, value) ->
            safeStore(key, value)
        }

        hetznerLabels.forEach { (key, value) ->
            safeStore(key, value)
        }
    }

    private val messageDigest = MessageDigest.getInstance("SHA-256")

    fun labels(): Map<String, String> {
        return safeLabels()
    }

    fun addLabel(key: String, value: String) {
        safeStore(key, value)
    }

    fun addHashedLabel(key: String, value: String) {
        safeStore(key, hashString(value))
    }

    fun hashLabelMatches(key: String, value: String): Boolean {
        return this.labels[key] == hashString(value)
    }

    private fun safeLabels(): Map<String, String> {

        data class IndexedLabelKey(val key: String, val index: Int)

        val labelsSortedByKeyAndIndex = labels.entries
            .map { it.key.split("_") to it.value }
            .map { (IndexedLabelKey(it.first[0], it.first.getOrElse(1) { "0" }.toInt())) to it.second }
            .sortedWith(compareBy({ it.first.key }, { it.first.index }))

        val map = HashMap<String, String>()

        labelsSortedByKeyAndIndex.forEach {
            if (map[it.first.key] == null) {
                map[it.first.key] = it.second
            } else {
                map[it.first.key] = "${map[it.first.key]}${it.second}"
            }
        }

        return map
    }

    private fun safeStore(key: String, value: String) {

        if (key.contains("_")) {
            throw RuntimeException("keys with underscores are not supported")
        }

        if (value.length > MAX_TOTAL_VALUE_LENGTH) {
            throw RuntimeException("labels values longer than $MAX_TOTAL_VALUE_LENGTH are not supported")
        }

        if (value.length >= MAX_VALUE_LENGTH) {
            value.chunked(MAX_VALUE_LENGTH).forEachIndexed { index, s ->
                this.labels["${key}_$index"] = s
            }
        } else {
            this.labels[key] = value
        }
    }

    private fun hashString(input: String): String {
        return messageDigest.digest(input.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }
    }
}
