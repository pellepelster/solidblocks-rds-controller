package de.solidblocks.rds.controller

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun testGenerateSshKey() {
        val result = Utils.generateSshKey("test")
        assertThat(result.first, startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
        assertThat(result.first, endsWith("-----END OPENSSH PRIVATE KEY-----\n"))
        assertThat(result.second, startsWith("ssh-ed25519"))
    }
}
