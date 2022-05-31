package de.solidblocks.rds.postgres

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class PostgresqlTest {

    @Test
    fun startsWithDefaultConfig() {

        val id = "docker-manager-${UUID.randomUUID()}"
        val dataDir = initWorldReadableTempDir(id)
        val backupDir = initWorldReadableTempDir(id)

        val instance = PostgresInstance(
            id = UUID.randomUUID(),
            database = "db1",
            dataDir = dataDir.toPath(),
            backupDir = backupDir.toPath()
        )

        assertThat(instance.start()).isTrue
        assertThat(instance.stop()).isTrue
    }
}
