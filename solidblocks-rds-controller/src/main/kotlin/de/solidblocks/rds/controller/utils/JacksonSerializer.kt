package de.solidblocks.rds.controller.utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kagkarlsson.scheduler.Serializer

class JacksonSerializer : Serializer {

    override fun serialize(data: Any?) = jacksonObjectMapper().writeValueAsBytes(data)

    override fun <T : Any?> deserialize(clazz: Class<T>?, serializedData: ByteArray?) =
        jacksonObjectMapper().readValue(serializedData, clazz)
}
