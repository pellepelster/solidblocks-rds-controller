package de.solidblocks.rds.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.rds.shared.defaultHttpClientBuilder
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.util.PemUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class MtlsHttpClient(
    val baseAddress: String,
    caPublicKeyRaw: String,
    privateKeyRaw: String,
    publicKeyRaw: String
) {
    val objectMapper = jacksonObjectMapper()

    val client: OkHttpClient

    init {
        val key = PemUtils.loadPrivateKey(privateKeyRaw.byteInputStream())

        val certificateFactory = CertificateFactory.getInstance("X.509")

        val caCertificate =
            certificateFactory.generateCertificate(caPublicKeyRaw.byteInputStream()) as X509Certificate
        val certificate =
            certificateFactory.generateCertificate(publicKeyRaw.byteInputStream()) as X509Certificate

        val sslFactory = SSLFactory.builder()
            .withIdentityMaterial(key, null, certificate)
            .withTrustMaterial(
                caCertificate
            ).build()

        client = defaultHttpClientBuilder().sslSocketFactory(
            sslFactory.sslSocketFactory,
            sslFactory.trustManager.get()
        )
            .retryOnConnectionFailure(false)
            .build()
    }

    inline fun <reified T> get(path: String): HttpResponse<T> {
        val type = objectMapper.typeFactory.constructType(T::class.java)

        val request = Request.Builder().url("$baseAddress/$path").build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, objectMapper.readValue(response.body?.bytes(), type))
    }

    inline fun <reified T> post(path: String, data: Any): HttpResponse<T> {
        val type = objectMapper.typeFactory.constructType(T::class.java)

        val request = Request.Builder()
            .post(jacksonObjectMapper().writeValueAsString(data).toRequestBody("application/json".toMediaTypeOrNull()))
            .url("$baseAddress/$path").build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, objectMapper.readValue(response.body?.bytes(), type))
    }

    fun post(path: String): HttpResponse<Any> {
        val request =
            Request.Builder().post("".toRequestBody("application/json".toMediaTypeOrNull())).url("$baseAddress/$path")
                .build()

        val response = client.newCall(request).execute()

        return HttpResponse(response.code, null)
    }
}
