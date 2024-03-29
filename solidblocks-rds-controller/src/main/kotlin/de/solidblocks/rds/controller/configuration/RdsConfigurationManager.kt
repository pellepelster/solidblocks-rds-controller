package de.solidblocks.rds.controller.configuration

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.instances.RdsInstancesManager
import de.solidblocks.rds.controller.model.entities.RdsConfigurationEntity
import de.solidblocks.rds.controller.model.entities.RdsInstanceEntity
import de.solidblocks.rds.controller.model.entities.RdsInstanceId
import de.solidblocks.rds.controller.model.repositories.RdsConfigurationRepository
import de.solidblocks.rds.controller.model.status.HealthStatus
import de.solidblocks.rds.controller.status.StatusManager
import de.solidblocks.rds.shared.dto.VersionResponse
import mu.KotlinLogging
import java.util.*

class RdsConfigurationManager(
    private val repository: RdsConfigurationRepository,
    private val rdsInstancesManager: RdsInstancesManager,
    private val rdsScheduler: RdsScheduler,
    private val statusManager: StatusManager
) {

    private val logger = KotlinLogging.logger {}

    private var applyTask = Tasks.oneTime(
        "rds-configuration-apply-task", RdsConfigurationEntity::class.java
    ).execute { inst: TaskInstance<RdsConfigurationEntity>, _: ExecutionContext ->
        // configureInstance(inst.data)
    }

    private var healthcheckTask = Tasks.recurring("rds-configuration-healthcheck-task", FixedDelay.ofSeconds(15))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            for (rdsConfiguration in repository.list()) {
                val endpoint = rdsInstancesManager.endpoint(rdsConfiguration.rdsInstance) ?: continue

                val client = MtlsHttpClient(
                    endpoint.agentAddress,
                    endpoint.caServerPublicKey,
                    endpoint.caClientPrivateKey,
                    endpoint.caClientPublicKey
                )

                try {
                    val response = client.get<VersionResponse>("/v1/agent/version")

                    if (response.isSuccessful) {
                        logger.info { "rds configuration '${rdsConfiguration.id}' is healthy" }
                        statusManager.update(rdsConfiguration.id.id, HealthStatus.HEALTHY)
                    } else {
                        logger.info { "rds configuration '${rdsConfiguration.id}' is unhealthy" }
                        statusManager.update(rdsConfiguration.id.id, HealthStatus.UNHEALTHY)
                    }
                } catch (_: Exception) {
                    logger.info { "rds configuration '${rdsConfiguration.id}' is unhealthy" }
                    statusManager.update(rdsConfiguration.id.id, HealthStatus.UNHEALTHY)
                }
            }
        }

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
    }

    fun create(rdsInstance: RdsInstanceId) = repository.create(rdsInstance)

    fun scheduleApply(instance: RdsInstanceEntity): Boolean {
        logger.info { "scheduling rds configuration apply for rds instance '${instance.name}'" }

        repository.list(instance.id).forEach {
            rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), it))
        }

        return true
    }

    fun list() = repository.list()

    fun deleteByInstance(id: UUID) = repository.deleteByInstance(id)

    /*
    private fun createHttpClient(id: UUID): MtlsHttpClient? {
        val instance = repository.read(id) ?: return null
        val provider = providersManager.read(instance.provider) ?: return null
        val controller = controllersManager.readInternal(provider.controller) ?: return null

        val instanceInfo = runningInstanceInfo(id) ?: return null

        return MtlsHttpClient(
            "https://${instanceInfo.ipAddress}:8080",
            controller.caServerPublicKey,
            controller.caClientPrivateKey,
            controller.caClientPublicKey
        )
    }
    fun runningInstancesStatus() = runningInstancesClients().map {
        try {
            val version = it.get<VersionResponse>("/v1/agent/version")
            if (version.isSuccessful) {
                return@map RunningInstanceStatus(version.data!!.version)
            }
        } catch (e: Exception) {
            return@map RunningInstanceStatus()
        }

        return@map RunningInstanceStatus()
    }

    fun ensureAll(): Boolean {
        return repository.list().map {
            if (diff(it)) {
                scheduleApply(it)
            } else {
                true
            }
        }.all { it }
    }

    private fun diff(rdsConfiguration: RdsConfigurationEntity): Boolean {
        val api = providersManager.createProviderApi(rdsInstance.provider) ?: return false

        if (!api.hasServer(serverName(rdsInstance))) {
            return true
        }

        return false
    }

    private fun configureInstance(rdsConfiguration: RdsConfigurationEntity): Boolean {
        val client = createHttpClient(rdsConfiguration.rdsInstance) ?: return false

        return true
    }
     */
}
