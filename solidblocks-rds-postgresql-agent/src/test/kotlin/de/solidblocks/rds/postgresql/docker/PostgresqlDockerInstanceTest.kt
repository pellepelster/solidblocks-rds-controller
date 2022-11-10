package de.solidblocks.rds.postgresql.docker

import de.solidblocks.rds.agent.initWorldReadableTempDir
import de.solidblocks.rds.postgresql.agent.PostgresqlDockerInstance
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class PostgresqlDockerInstanceTest {
    @Test
    fun startsWithDefaultConfig() {

        val id = "docker-manager-${UUID.randomUUID()}"

        val dataDir = initWorldReadableTempDir(id)
        val backupDir = initWorldReadableTempDir(id)

        val instance = PostgresqlDockerInstance(
            id = UUID.randomUUID(),
            databaseName = "database1",
            dataDir = dataDir.toPath(),
            backupDir = backupDir.toPath(),
            username = "user1",
            password = "password1"
        )

        assertThat(instance.start()).isTrue

        assertThat(instance.stop()).isTrue
    }
}
