package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.controllers.ControllerEntity
import de.solidblocks.rds.controller.model.controllers.ControllersRepository
import de.solidblocks.rds.controller.model.providers.ProviderStatus
import de.solidblocks.rds.controller.model.providers.ProvidersRepository
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.PROVIDERS
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(ManagementTestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProvidersRepositoryTest {

    lateinit var controller: ControllerEntity

    @BeforeEach
    fun beforeAll(database: Database) {
        database.dsl.deleteFrom(CONFIGURATION_VALUES).execute()
        database.dsl.deleteFrom(PROVIDERS).execute()

        val controllersRepository = ControllersRepository(database.dsl)
        controller = controllersRepository.create("controller-${UUID.randomUUID()}")
    }

    @Test
    fun testExists(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        assertThat(repository.exists("exists")).isFalse
        repository.create("exists", controller)
        assertThat(repository.exists("exists")).isTrue
    }

    @Test
    fun testDelete(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val entity = repository.create("delete", controller)
        assertThat(repository.exists("delete")).isTrue
        assertThat(repository.listDeleted()).isEmpty()

        repository.delete(entity.id)
        assertThat(repository.exists("delete")).isFalse
        assertThat(repository.listDeleted()).isNotEmpty
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val entity = repository.create("delete-with-config-values", controller, mapOf("key1" to "value1"))
        assertThat(repository.exists("delete-with-config-values")).isTrue

        repository.delete(entity.id)
        assertThat(repository.exists("delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        repository.create("update-config-value", controller)

        assertThat(repository.update("non-existing", "key1", "value1")).isFalse
        assertThat(repository.update("update-config-value", "key1", "value1")).isTrue

        val entity = repository.read("update-config-value")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("key1")
        assertThat(entity.configValues[0].value).isEqualTo("value1")
        assertThat(entity.configValues[0].version).isEqualTo(0)
        assertThat(entity.configValues).hasSize(1)

        assertThat(repository.update("update-config-value", "key1", "value2")).isTrue

        val updatedEntity = repository.read("update-config-value")!!
        assertThat(updatedEntity).isNotNull
        assertThat(updatedEntity.configValues[0].name).isEqualTo("key1")
        assertThat(updatedEntity.configValues[0].value).isEqualTo("value2")
        assertThat(updatedEntity.configValues[0].version).isEqualTo(1)
        assertThat(updatedEntity.configValues).hasSize(1)

        assertThat(repository.update("update-config-value", "key1", null)).isTrue
        val updatedInstanceWithNullValue = repository.read("update-config-value")!!
        assertThat(updatedInstanceWithNullValue.configValues[0].value).isNull()
    }

    @Test
    fun testCreate(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        assertThat(repository.read("create")).isNull()
        repository.create("create", controller, mapOf("abc" to "def"))

        val entity = repository.read("create")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("abc")
        assertThat(entity.configValues[0].value).isEqualTo("def")
        assertThat(entity.configValues).hasSize(1)
        assertThat(entity.status).isEqualTo(ProviderStatus.UNKNOWN)
    }

    @Test
    fun testUpdateStatus(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val created = repository.create("update-status", controller)
        assertThat(created.status).isEqualTo(ProviderStatus.UNKNOWN)

        val entity = repository.read("update-status")!!
        assertThat(entity.status).isEqualTo(ProviderStatus.UNKNOWN)

        repository.updateStatus(entity.id, ProviderStatus.ERROR)

        val updated = repository.read("update-status")!!
        assertThat(updated.status).isEqualTo(ProviderStatus.ERROR)

        repository.resetStatus()

        val afterReset = repository.read("update-status")!!
        assertThat(afterReset.status).isEqualTo(ProviderStatus.UNKNOWN)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        repository.create("create-without-config-values", controller)

        val instance = repository.read("create-without-config-values")!!

        assertThat(instance).isNotNull
        assertThat(instance.configValues).hasSize(0)
    }
}
