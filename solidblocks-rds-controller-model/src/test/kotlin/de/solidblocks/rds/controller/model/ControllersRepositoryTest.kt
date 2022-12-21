package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.model.controllers.ControllersRepository
import de.solidblocks.rds.test.TestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ControllersRepositoryTest {

    @Test
    fun testDelete(database: Database) {
        val repository = ControllersRepository(database.dsl)

        val entity = repository.create("delete")
        assertThat(repository.exists("delete")).isTrue

        assertThat(repository.delete(entity.id)).isTrue
        assertThat(repository.exists("delete")).isFalse
    }

    @Test
    fun testDeleteWithConfigValues(database: Database) {
        val repository = ControllersRepository(database.dsl)

        val entity =
            repository.create("delete-with-config-values", mapOf("key1" to "label1"))
        assertThat(repository.exists("delete-with-config-values")).isTrue

        repository.delete(entity.id)
        assertThat(repository.exists("delete-with-config-values")).isFalse
    }

    @Test
    fun testUpdate(database: Database) {
        val repository = ControllersRepository(database.dsl)

        repository.create("update-config-value")

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
        val repository = ControllersRepository(database.dsl)

        assertThat(repository.read("create")).isNull()
        repository.create("create", mapOf("abc" to "def"))

        val entity = repository.read("create")!!
        assertThat(entity).isNotNull
        assertThat(entity.configValues[0].name).isEqualTo("abc")
        assertThat(entity.configValues[0].value).isEqualTo("def")
        assertThat(entity.configValues).hasSize(1)
    }

    @Test
    fun testCreateWithoutConfigValues(database: Database) {
        val repository = ControllersRepository(database.dsl)

        repository.create("create-without-config-values")

        val entity = repository.read("create-without-config-values")!!

        assertThat(entity).isNotNull
        assertThat(entity.configValues).hasSize(0)
    }
}
