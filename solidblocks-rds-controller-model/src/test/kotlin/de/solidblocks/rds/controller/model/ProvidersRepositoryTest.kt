package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.PROVIDERS
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ManagementTestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProvidersRepositoryTest {

    @BeforeEach
    fun beforeAll(database: Database) {
        database.dsl.deleteFrom(CONFIGURATION_VALUES).execute()
        database.dsl.deleteFrom(PROVIDERS).execute()
    }

    @Test
    fun testExists(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        assertThat(repository.exists("provider-exists")).isFalse
        repository.create("provider-exists")
        assertThat(repository.exists("provider-exists")).isTrue
    }

    @Test
    fun testDelete(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val provider = repository.create("provider-delete")
        assertThat(repository.exists("provider-delete")).isTrue
        assertThat(repository.listDeleted()).isEmpty()

        repository.delete(provider!!.id)
        assertThat(repository.exists("provider-delete")).isFalse
        assertThat(repository.listDeleted()).isNotEmpty
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val provider = repository.create("provider-delete-with-config-values", mapOf("key1" to "value1"))
        assertThat(repository.exists("provider-delete-with-config-values")).isTrue

        repository.delete(provider!!.id)
        assertThat(repository.exists("provider-delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        repository.create("provider-update-config-value")

        assertThat(repository.update("non-existing-instance", "key1", "value1")).isFalse
        assertThat(repository.update("provider-update-config-value", "key1", "value1")).isTrue
        val instance = repository.read("provider-update-config-value")!!
        assertThat(instance).isNotNull
        assertThat(instance.configValues[0].name).isEqualTo("key1")
        assertThat(instance.configValues[0].value).isEqualTo("value1")
        assertThat(instance.configValues[0].version).isEqualTo(0)
        assertThat(instance.configValues).hasSize(1)

        assertThat(repository.update("provider-update-config-value", "key1", "value2")).isTrue
        val updatedInstance = repository.read("provider-update-config-value")!!
        assertThat(updatedInstance).isNotNull
        assertThat(updatedInstance.configValues[0].name).isEqualTo("key1")
        assertThat(updatedInstance.configValues[0].value).isEqualTo("value2")
        assertThat(updatedInstance.configValues[0].version).isEqualTo(1)
        assertThat(updatedInstance.configValues).hasSize(1)

        assertThat(repository.update("provider-update-config-value", "key1", null)).isTrue
        val updatedInstanceWithNullValue = repository.read("provider-update-config-value")!!
        assertThat(updatedInstanceWithNullValue.configValues[0].value).isNull()
    }

    @Test
    fun testCreate(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        assertThat(repository.read("provider1")).isNull()
        repository.create("provider1", mapOf("abc" to "def"))

        val provider1 = repository.read("provider1")!!
        assertThat(provider1).isNotNull
        assertThat(provider1.configValues[0].name).isEqualTo("abc")
        assertThat(provider1.configValues[0].value).isEqualTo("def")
        assertThat(provider1.configValues).hasSize(1)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        repository.create("provider-without-config-values")

        val instance = repository.read("provider-without-config-values")!!

        assertThat(instance).isNotNull
        assertThat(instance.configValues).hasSize(0)
    }
}
