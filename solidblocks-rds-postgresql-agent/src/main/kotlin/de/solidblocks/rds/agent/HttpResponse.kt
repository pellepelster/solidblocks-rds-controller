package de.solidblocks.rds.agent

class HttpResponse<T>(val code: Int, val data: T?) {
    val isSuccessful: Boolean
        get() = code in 200..299
}
