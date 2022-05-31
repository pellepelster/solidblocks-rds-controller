package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.model.ProvidersRepository
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ManagementTestDatabaseExtension::class)
class ProvidersManagerTest {

    @Test
    fun testCreate(database: Database) {
        val repository = ProvidersRepository(database.dsl)

        val manager = ProvidersManager(repository, mockk<RdsInstancesManager>())

        val request = ProviderCreateRequest("name1", "apiKey1")
        val created = manager.create(request)

        val fetched = repository.read(created.data!!.id)!!

        assertThat(fetched.name).isEqualTo("name1")
        assertThat(fetched.apiKey()).isEqualTo("apiKey1")
        assertThat(fetched.apiKey()).isEqualTo("apiKey1")

        assertThat(fetched.sshPublicKey()).startsWith("ssh-ed25519")
        assertThat(fetched.sshPrivateKey()).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
    }
}
