package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.status.Status
import de.solidblocks.rds.controller.model.status.StatusRepository
import de.solidblocks.rds.controller.model.tables.references.STATUS
import de.solidblocks.rds.test.TestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatusRepositoryTest {

    @BeforeEach
    fun beforeAll(database: Database) {
        database.dsl.deleteFrom(STATUS).execute()
    }

    @Test
    fun testUpdateStatus(database: Database) {
        val repository = StatusRepository(database.dsl)

        val id = UUID.randomUUID()

        assertThat(repository.latest(id)).isNull()

        repository.update(id, Status.HEALTHY)
        assertThat(repository.latest(id)!!.status).isEqualTo(Status.HEALTHY.toString())

        repository.update(id, Status.UNHEALTHY)
        assertThat(repository.latest(id)!!.status).isEqualTo(Status.UNHEALTHY.toString())
    }
}
