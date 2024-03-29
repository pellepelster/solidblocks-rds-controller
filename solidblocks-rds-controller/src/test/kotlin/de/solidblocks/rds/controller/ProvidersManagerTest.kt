package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.model.repositories.ControllersRepository
import de.solidblocks.rds.controller.model.repositories.LogRepository
import de.solidblocks.rds.controller.model.repositories.ProvidersRepository
import de.solidblocks.rds.controller.model.repositories.RdsInstancesRepository
import de.solidblocks.rds.controller.model.status.StatusRepository
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.status.StatusManager
import de.solidblocks.rds.test.TestDatabaseExtension
import io.mockk.justRun
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestDatabaseExtension::class)
class ProvidersManagerTest {

    @Test
    fun testCreate(database: Database) {
        val repository = ProvidersRepository(database.dsl)
        val rdsInstancesRepository = RdsInstancesRepository(database.dsl)
        val logRepository = LogRepository(database.dsl)

        val controllersManager = ControllersManager(ControllersRepository(database.dsl))

        val rdsScheduler = mockk<RdsScheduler>()
        justRun { rdsScheduler.addOneTimeTask(any()) }
        justRun { rdsScheduler.addRecurringTask(any()) }
        justRun { rdsScheduler.scheduleTask(any()) }

        val statusManager = StatusManager(StatusRepository(database.dsl))

        val manager = ProvidersManager(
            repository, rdsInstancesRepository, controllersManager, rdsScheduler,
            statusManager, logRepository
        )

        val request = ProviderCreateRequest("name1", "apiKey1")
        val created = manager.create(request)

        val fetched = repository.read(created.data!!.id)!!

        assertThat(fetched.name).isEqualTo("name1")
        assertThat(fetched.apiKey).isEqualTo("apiKey1")
        assertThat(fetched.apiKey).isEqualTo("apiKey1")

        assertThat(fetched.sshPublicKey).startsWith("ssh-ed25519")
        assertThat(fetched.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
    }
}
