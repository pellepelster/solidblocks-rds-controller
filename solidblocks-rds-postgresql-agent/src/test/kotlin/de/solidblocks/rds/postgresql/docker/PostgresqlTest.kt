package de.solidblocks.rds.postgresql.docker

import de.solidblocks.rds.agent.initWorldReadableTempDir
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class PostgresqlTest {

    @Test
    fun startsWithDefaultConfig() {

        val id = "docker-manager-${UUID.randomUUID()}"
        val dataDir = initWorldReadableTempDir(id)
        val backupDir = initWorldReadableTempDir(id)

        val instance = PostgresqlDockerInstance(
            id = UUID.randomUUID(),
            database = "db1",
            dataDir = dataDir.toPath(),
            backupDir = backupDir.toPath()
        )

        Assertions.assertThat(instance.start()).isTrue
        Assertions.assertThat(instance.stop()).isTrue
    }
}
