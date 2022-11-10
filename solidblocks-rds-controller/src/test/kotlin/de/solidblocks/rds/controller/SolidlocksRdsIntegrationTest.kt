package de.solidblocks.rds.controller

import de.solidblocks.rds.base.Database
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.model.controllers.ControllersRepository
import de.solidblocks.rds.controller.model.instances.RdsInstancesRepository
import de.solidblocks.rds.controller.model.providers.ProvidersRepository
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.shared.dto.VersionResponse
import de.solidblocks.rds.shared.solidblocksVersion
import de.solidblocks.rds.test.ManagementTestDatabaseExtension
import io.mockk.justRun
import io.mockk.mockk
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds
import kotlin.io.path.exists
import kotlin.io.path.readText

@EnabledIfEnvironmentVariable(named = "HCLOUD_TOKEN", matches = ".*")
@ExtendWith(ManagementTestDatabaseExtension::class)
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

        val rdsScheduler = mockk<RdsScheduler>()
        justRun { rdsScheduler.addOneTimeTask(any()) }
        justRun { rdsScheduler.addRecurringTask(any()) }
        justRun { rdsScheduler.scheduleTask(any()) }

        val controllersManager = ControllersManager(ControllersRepository(database.dsl))
        val providersManager = ProvidersManager(
            ProvidersRepository(database.dsl),
            RdsInstancesRepository(database.dsl),
            controllersManager,
            rdsScheduler
        )

        val rdsInstancesManager = RdsInstancesManager(
            RdsInstancesRepository(database.dsl),
            providersManager,
            controllersManager,
            rdsScheduler
        )

        val provider =
            providersManager.create(ProviderCreateRequest(name = "hetzner1", apiKey = System.getenv("HCLOUD_TOKEN")))
        rdsInstancesManager.create(RdsInstanceCreateRequest(name = "rds-instance1", provider.data!!.id))

        assertThat(providersManager.applyAll()).isTrue
        assertThat(rdsInstancesManager.applyAll()).isTrue

        await().atMost(ofMinutes(2)).pollInterval(ofSeconds(5)).until({
            rdsInstancesManager.runningInstancesStatus()
        }, { it.isNotEmpty() && it.all { it.status != null } })

        rdsInstancesManager.runningInstancesClients().forEach {
            val version = it.get<VersionResponse>("/v1/agent/version")
            assertThat(version.code).isEqualTo(200)
            assertThat(version.data!!.version).isEqualTo(solidblocksVersion())
        }
    }
}
