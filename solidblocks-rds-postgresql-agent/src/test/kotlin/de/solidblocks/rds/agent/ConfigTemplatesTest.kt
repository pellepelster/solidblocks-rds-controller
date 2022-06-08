package de.solidblocks.rds.agent

import de.solidblocks.rds.docker.ConfigTemplates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ConfigTemplatesTest {
    @Test
    fun testPgHbaConf(@TempDir tempDir: Path) {
        ConfigTemplates().pgHbaConf(tempDir, "user1")
        assertThat(tempDir.resolve("pg_hba.conf")).exists()
    }
}
