package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.utils.Utils
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
    fun testX509CertificateGeneration() {
        val result = Utils.generateX509Certificate()

        assertThat(result.privateKey, startsWith("-----BEGIN EC PRIVATE KEY-----"))
        assertThat(result.privateKey, endsWith("-----END EC PRIVATE KEY-----\n"))

        assertThat(result.publicKey, startsWith("-----BEGIN CERTIFICATE-----"))
        assertThat(result.publicKey, endsWith("-----END CERTIFICATE-----\n"))
    }
}
