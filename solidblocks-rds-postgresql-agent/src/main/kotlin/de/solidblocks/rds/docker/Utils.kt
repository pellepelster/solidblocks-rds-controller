package de.solidblocks.rds.docker

import okhttp3.OkHttpClient
import java.time.Duration

fun defaultHttpClient() = defaultHttpClientBuilder().build()

fun defaultHttpClientBuilder() = OkHttpClient.Builder()
    .callTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(10))
    .connectTimeout(Duration.ofSeconds(10))
