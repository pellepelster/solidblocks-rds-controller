package de.solidblocks.rds.cloudinit

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CloudInitTemplatesTest {

    @Test
    fun testSolidblocksRdsCloudInit(@TempDir tempDir: Path) {
        CloudInitTemplates.solidblocksRdsCloudInit(
            "version123",
            "/dev/disk/abc",
            "host124",
            "pellepelster",
            "pat123",
            "ca-public-key",
            "server-private-key",
            "server-public-key",
            "solidblocks-rds-postgresql-agent"
        )
        // assertThat(tempDir.resolve("solidblocks-rds-cloud-init.sh")).exists()
    }
}
