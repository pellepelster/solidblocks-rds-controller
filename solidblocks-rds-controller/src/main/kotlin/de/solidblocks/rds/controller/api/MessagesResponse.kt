package de.solidblocks.rds.controller.api

import kotlin.reflect.KProperty1

data class MessagesResponse(val messages: List<MessageResponse>) {

    fun hasErrors() = messages.isNotEmpty()

    companion object {
        fun ok() = MessagesResponse(emptyList())

        fun error(code: String) = MessagesResponse(listOf(MessageResponse(code = code)))

        fun error(attribute: KProperty1<*, *>, code: String) =
            MessagesResponse(code.messageResponses(attribute))
    }
}
