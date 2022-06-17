package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.controllers.ControllersRepository
import de.solidblocks.rds.controller.model.instances.RdsInstancesRepository
import de.solidblocks.rds.controller.model.providers.ProviderStatus
import de.solidblocks.rds.controller.model.providers.ProvidersRepository
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(ManagementTestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RdsInstancesRepositoryTest {

    lateinit var providerId: UUID

    @BeforeAll
    fun beforeAll(database: Database) {
        val providersRepository = ProvidersRepository(database.dsl)

        val controllersRepository = ControllersRepository(database.dsl)
        val controller = controllersRepository.create("controller-${UUID.randomUUID()}")

        val provider = providersRepository.create("provider-${UUID.randomUUID()}", controller)
        providerId = provider.id
    }

    @Test
    fun testDelete(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val entity = repository.create(providerId, "delete")
        assertThat(repository.exists("delete")).isTrue

        repository.delete(entity.id)
        assertThat(repository.exists("delete")).isFalse
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val entity =
            repository.create(providerId, "delete-with-config-values", mapOf("key1" to "label1"))
        assertThat(repository.exists("delete-with-config-values")).isTrue

        repository.delete(entity.id)
        assertThat(repository.exists("delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId, "update-config-value")

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
        val repository = RdsInstancesRepository(database.dsl)

        assertThat(repository.read("create")).isNull()
        repository.create(providerId, "create", mapOf("abc" to "def"))

        val entity = repository.read("create")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("abc")
        assertThat(entity.configValues[0].value).isEqualTo("def")
        assertThat(entity.configValues).hasSize(1)
        assertThat(entity.status).isEqualTo(ProviderStatus.UNKNOWN)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId, "create-without-config-values")

        val entity = repository.read("create-without-config-values")!!

        assertThat(entity).isNotNull
        assertThat(entity.configValues).hasSize(0)
    }

    @Test
    fun testUpdateStatus(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val created = repository.create(providerId, "update-status")
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
}
