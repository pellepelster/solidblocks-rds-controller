package de.solidblocks.rds.postgresql.agent

import de.solidblocks.rds.agent.initWorldReadableTempDir
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RdsInstancesManagerTest {

    @Test
    fun testManageRdsInstances() {

        val dataDir = initWorldReadableTempDir(UUID.randomUUID().toString())
        val backupDir = initWorldReadableTempDir(UUID.randomUUID().toString())

        val rdsInstancesManager = RdsInstancesManager(dataDir.toPath(), backupDir.toPath())
        assertThat(rdsInstancesManager.list()).hasSize(0)

        val id = UUID.randomUUID()
        assertThat(rdsInstancesManager.create(id, "database1", "user1", "password1")).isTrue
        assertThat(rdsInstancesManager.list()).hasSize(1)

        assertThat(rdsInstancesManager.create(id, "database1", "user1", "password1")).isFalse
        val rdsInstance = rdsInstancesManager.get(id)

        assertThat(rdsInstance).isNotNull
        assertThat(rdsInstance!!.databaseName).isEqualTo("database1")
        assertThat(rdsInstance.username).isEqualTo("user1")
        assertThat(rdsInstance.password).isEqualTo("password1")
    }
}
