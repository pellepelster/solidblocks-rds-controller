package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
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
        val repository = ProvidersRepository(database.dsl)
        val provider = repository.create("provider-${UUID.randomUUID()}")
        providerId = provider!!.id
    }

    @Test
    fun testDelete(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val provider = repository.create(providerId, "rds-instances-delete")
        assertThat(repository.exists("rds-instances-delete")).isTrue

        repository.delete(provider!!.id)
        assertThat(repository.exists("rds-instances-delete")).isFalse
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        val provider =
            repository.create(providerId, "rds-instances-delete-with-config-values", mapOf("key1" to "label1"))
        assertThat(repository.exists("rds-instances-delete-with-config-values")).isTrue

        repository.delete(provider!!.id)
        assertThat(repository.exists("rds-instances-delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId, "instance-update-config-value")

        assertThat(repository.update("non-existing-instance", "key1", "value1")).isFalse
        assertThat(repository.update("instance-update-config-value", "key1", "value1")).isTrue
        val instance = repository.read("instance-update-config-value")!!
        assertThat(instance).isNotNull
        assertThat(instance.configValues[0].name).isEqualTo("key1")
        assertThat(instance.configValues[0].value).isEqualTo("value1")
        assertThat(instance.configValues[0].version).isEqualTo(0)
        assertThat(instance.configValues).hasSize(1)

        assertThat(repository.update("instance-update-config-value", "key1", "value2")).isTrue
        val updatedInstance = repository.read("instance-update-config-value")!!
        assertThat(updatedInstance).isNotNull
        assertThat(updatedInstance.configValues[0].name).isEqualTo("key1")
        assertThat(updatedInstance.configValues[0].value).isEqualTo("value2")
        assertThat(updatedInstance.configValues[0].version).isEqualTo(1)
        assertThat(updatedInstance.configValues).hasSize(1)

        assertThat(repository.update("instance-update-config-value", "key1", null)).isTrue
        val updatedInstanceWithNullValue = repository.read("instance-update-config-value")!!
        assertThat(updatedInstanceWithNullValue.configValues[0].value).isNull()
    }

    @Test
    fun testCreate(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        assertThat(repository.read("instance1")).isNull()
        repository.create(providerId, "instance1", mapOf("abc" to "def"))

        val instance1 = repository.read("instance1")!!
        assertThat(instance1).isNotNull
        assertThat(instance1.configValues[0].name).isEqualTo("abc")
        assertThat(instance1.configValues[0].value).isEqualTo("def")
        assertThat(instance1.configValues).hasSize(1)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create(providerId, "instance-without-config-values")

        val instance = repository.read("instance-without-config-values")!!

        assertThat(instance).isNotNull
        assertThat(instance.configValues).hasSize(0)
    }
}
