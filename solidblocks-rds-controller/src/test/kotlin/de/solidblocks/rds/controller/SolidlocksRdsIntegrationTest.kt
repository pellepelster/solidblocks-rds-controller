package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.configuration.RdsConfigurationManager
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.model.repositories.ControllersRepository
import de.solidblocks.rds.controller.model.repositories.ProvidersRepository
import de.solidblocks.rds.controller.model.repositories.RdsConfigurationRepository
import de.solidblocks.rds.controller.model.repositories.RdsInstancesRepository
import de.solidblocks.rds.controller.model.status.Status
import de.solidblocks.rds.controller.model.status.StatusManager
import de.solidblocks.rds.controller.model.status.StatusRepository
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.test.TestDatabaseExtension
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import java.time.Duration.ofMinutes
import kotlin.io.path.exists
import kotlin.io.path.readText

@EnabledIfEnvironmentVariable(named = "HCLOUD_TOKEN", matches = ".*")
@ExtendWith(TestDatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SolidlocksRdsIntegrationTest {

    private val logger = KotlinLogging.logger {}

    private val hetznerApi = HetznerApi(System.getenv("HCLOUD_TOKEN"))

    private val hetznerCloudAPI = HetznerCloudAPI(System.getenv("HCLOUD_TOKEN"))

    @BeforeAll
    fun beforeAll() {
        cleanTestbed()

        val idRsaPub = Path.of("${System.getProperty("user.home")}/.ssh/id_rsa.pub")
        if (idRsaPub.exists()) {
            logger.info { "adding debug ssh key '$idRsaPub'" }
            val debugSshKeyName = System.getProperty("user.name")
            hetznerApi.ensureSSHKey(debugSshKeyName, idRsaPub.readText())
        }
    }

    @AfterAll
    fun afterAll() {
        cleanTestbed()
    }

    fun cleanTestbed() {
        assertThat(hetznerApi.deleteAllSSHKeys()).isTrue
        assertThat(hetznerApi.deleteAllVolumes()).isTrue

        hetznerCloudAPI.servers.servers.forEach {
            hetznerCloudAPI.deleteServer(it.id)
        }
    }

    @Test
    fun testCreateRdsInstance(database: Database) {
        val rdsScheduler = RdsScheduler(database)
        val statusManager = StatusManager(StatusRepository(database.dsl))

        val rdsInstancesRepository = RdsInstancesRepository(database.dsl)

        val controllersManager = ControllersManager(ControllersRepository(database.dsl))

        val providersManager = ProvidersManager(
            ProvidersRepository(database.dsl),
            rdsInstancesRepository,
            controllersManager,
            rdsScheduler,
            statusManager
        )

        val rdsInstancesManager = RdsInstancesManager(
            rdsInstancesRepository,
            providersManager,
            controllersManager,
            rdsScheduler,
            statusManager
        )

        val rdsConfigurationManager = RdsConfigurationManager(
            RdsConfigurationRepository(database.dsl),
            rdsInstancesManager,
            rdsScheduler,
            statusManager
        )

        val rdsManager = RdsManager(database.dsl, rdsInstancesManager, rdsConfigurationManager)

        rdsScheduler.start()

        val provider =
            providersManager.create(ProviderCreateRequest(name = "hetzner1", apiKey = System.getenv("HCLOUD_TOKEN")))

        await().atMost(ofMinutes(2)).until {
            providersManager.list().isNotEmpty() && providersManager.list().all {
                statusManager.latest(it.id) == Status.HEALTHY
            }
        }

        rdsManager.create(
            RdsInstanceCreateRequest(
                name = "rds-instance1",
                username = "user1",
                password = "password1",
                provider = provider.data!!.id
            )
        )

        await().atMost(ofMinutes(3)).until {
            rdsConfigurationManager.list().isNotEmpty() && rdsConfigurationManager.list().all {
                statusManager.latest(it.id.id) == Status.HEALTHY
            }
        }
    }
}
