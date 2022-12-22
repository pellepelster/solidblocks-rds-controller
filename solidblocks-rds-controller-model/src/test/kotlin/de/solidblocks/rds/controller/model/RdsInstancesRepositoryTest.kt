package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.entities.ProviderId
import de.solidblocks.rds.controller.model.repositories.ControllersRepository
import de.solidblocks.rds.controller.model.repositories.ProvidersRepository
import de.solidblocks.rds.controller.model.repositories.RdsInstancesRepository
import de.solidblocks.rds.test.TestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(TestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RdsInstancesRepositoryTest {

    lateinit var providerId: ProviderId

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

        val entity = repository.create(providerId.id, "delete", "user1", "password1", "delete", "privatekey")
        assertThat(repository.exists("delete")).isTrue

        repository.delete(entity.id.id)
        assertThat(repository.exists("delete")).isFalse
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val entity =
            repository.create(
                providerId.id,
                "delete-with-config-values",
                "user1",
                "password1",
                "privatekey",
                "publickey",
                mapOf("key1" to "label1")
            )
        assertThat(repository.exists("delete-with-config-values")).isTrue

        repository.delete(entity.id.id)
        assertThat(repository.exists("delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId.id, "update-config-value", "user1", "password1", "privatekey", "publickey")

        assertThat(repository.update("non-existing", "key1", "value1")).isFalse
        assertThat(repository.update("update-config-value", "key1", "value1")).isTrue

        val entity = repository.read("update-config-value")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("key1")
        assertThat(entity.configValues[0].value).isEqualTo("value1")
        assertThat(entity.configValues[0].version).isEqualTo(0)
        assertThat(entity.configValues).hasSize(5)

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
        repository.create(providerId.id, "create", "user1", "password1", "privatekey", "publickey", mapOf("abc" to "def"))

        val entity = repository.read("create")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("abc")
        assertThat(entity.configValues[0].value).isEqualTo("def")
        assertThat(entity.configValues).hasSize(5)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId.id, "create-without-config-values", "user1", "password1", "privatekey", "publickey")

        val entity = repository.read("create-without-config-values")!!

        assertThat(entity).isNotNull
        assertThat(entity.configValues).hasSize(4)
    }

    @Test
    fun testCount(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val currentTotalCount = repository.count()
        val currentCount = repository.count(providerId.id)

        repository.create(providerId.id, "count", "user1", "password1", "privatekey", "publickey")

        assertThat(repository.count()).isEqualTo(currentTotalCount + 1)
        assertThat(repository.count(providerId.id)).isEqualTo(currentCount + 1)
    }
}
