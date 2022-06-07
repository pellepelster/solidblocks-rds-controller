package de.solidblocks.rds.base

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun testGenerateSshKey() {
        val result = Utils.generateSshKey("test")

        assertThat(result.privateKey, startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
        assertThat(result.privateKey, endsWith("-----END OPENSSH PRIVATE KEY-----\n"))

        assertThat(result.publicKey, startsWith("ssh-ed25519"))
    }

    @Test
    fun testGenerateCertificate() {
        val caResult = Utils.generateCAKeyPAir()

        assertThat(caResult.privateKey, startsWith("-----BEGIN PRIVATE KEY-----"))
        assertThat(caResult.privateKey, endsWith("-----END PRIVATE KEY-----\n"))

        assertThat(caResult.publicKey, startsWith("-----BEGIN CERTIFICATE-----"))
        assertThat(caResult.publicKey, endsWith("-----END CERTIFICATE-----\n"))

        val certificateResult = Utils.createCertificate(caResult.privateKey, caResult.publicKey)

        assertThat(certificateResult.privateKey, startsWith("-----BEGIN PRIVATE KEY-----"))
        assertThat(certificateResult.privateKey, endsWith("-----END PRIVATE KEY-----\n"))

        assertThat(certificateResult.publicKey, startsWith("-----BEGIN CERTIFICATE-----"))
        assertThat(certificateResult.publicKey, endsWith("-----END CERTIFICATE-----\n"))
    }
}
