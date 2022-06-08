package de.solidblocks.rds.cloudinit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CloudInitTemplatesTest {

    @Test
    fun testSolidblocksRdsCloudInit(@TempDir tempDir: Path) {
        CloudInitTemplates().solidblocksRdsCloudInit(tempDir)

        assertThat(tempDir.resolve("solidblocks-rds-cloud-init.sh")).exists()
    }
}
