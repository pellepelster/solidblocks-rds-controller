package de.solidblocks.rds.controller.model

import de.solidblocks.rds.base.ManagementDatabase
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ManagementTestDatabaseExtension::class)
class RdsInstancesRepositoryTest {

    @Test
    fun testUpdate(database: ManagementDatabase) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create("instance-update-config-value")

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
    fun testCreate(database: ManagementDatabase) {
        val repository = RdsInstancesRepository(database.dsl)

        assertThat(repository.read("instance1")).isNull()
        repository.create("instance1", listOf("abc" to "def"))

        val instance1 = repository.read("instance1")!!
        assertThat(instance1).isNotNull
        assertThat(instance1.configValues[0].name).isEqualTo("abc")
        assertThat(instance1.configValues[0].value).isEqualTo("def")
        assertThat(instance1.configValues).hasSize(1)
    }

    @Test
    fun testCreateWithoutConfigValues(database: ManagementDatabase) {
        val repository = RdsInstancesRepository(database.dsl)

        repository.create("instance-without-config-values")

        val instance = repository.read("instance-without-config-values")!!

        assertThat(instance).isNotNull
        assertThat(instance.configValues).hasSize(0)
    }

}
