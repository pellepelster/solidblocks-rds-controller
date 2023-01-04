package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.repositories.LogRepository
import de.solidblocks.rds.controller.model.status.HealthStatus
import de.solidblocks.rds.controller.model.status.ProvisioningStatus
import de.solidblocks.rds.controller.model.status.StatusRepository
import de.solidblocks.rds.controller.model.tables.references.LOG
import de.solidblocks.rds.controller.model.tables.references.STATUS
import de.solidblocks.rds.test.TestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.event.Level
import java.util.*

@ExtendWith(TestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogRepositoryTest {

    @BeforeEach
    fun beforeAll(database: Database) {
        database.dsl.deleteFrom(LOG).execute()
    }

    @Test
    fun testLog(database: Database) {
        val repository = LogRepository(database.dsl)

        val id = UUID.randomUUID()

        repository.log(id, Level.INFO, "some message")
    }

}
