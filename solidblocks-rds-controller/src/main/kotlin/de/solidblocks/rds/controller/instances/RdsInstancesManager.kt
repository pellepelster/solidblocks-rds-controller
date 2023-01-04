package de.solidblocks.rds.controller.instances

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay.ofSeconds
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.cloudinit.CloudInitTemplates
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.MessagesResponse
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.model.entities.ProviderId
import de.solidblocks.rds.controller.model.entities.RdsInstanceEntity
import de.solidblocks.rds.controller.model.entities.RdsInstanceId
import de.solidblocks.rds.controller.model.repositories.RdsInstancesRepository
import de.solidblocks.rds.controller.model.status.Status
import de.solidblocks.rds.controller.model.status.StatusManager
import de.solidblocks.rds.controller.providers.AgentEndpoint
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.utils.Constants
import de.solidblocks.rds.controller.utils.Constants.backup1VolumeName
import de.solidblocks.rds.controller.utils.Constants.data1VolumeName
import de.solidblocks.rds.controller.utils.Constants.serverName
import de.solidblocks.rds.controller.utils.ErrorCodes
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.docker.HealthChecks
import de.solidblocks.rds.shared.solidblocksVersion
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.*

class RdsInstancesManager(
    private val repository: RdsInstancesRepository,
    private val providersManager: ProvidersManager,
    private val controllersManager: ControllersManager,
    private val rdsScheduler: RdsScheduler,
    private val statusManager: StatusManager
) {

    private val logger = KotlinLogging.logger {}

    private var applyTask = Tasks.oneTime(
        "rds-instances-apply-task", RdsInstanceEntity::class.java
    ).execute { inst: TaskInstance<RdsInstanceEntity>, ctx: ExecutionContext ->
        apply(inst.data)
    }

    private var ensureTask =
        Tasks.recurring("rds-instances-ensure-task", ofSeconds(60))
            .execute { _: TaskInstance<Void>, _: ExecutionContext ->
                ensureAll()
            }

    private var healthcheckTask = Tasks.recurring("rds-instances-healthcheck-task", ofSeconds(15))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            for (rdsInstance in repository.list()) {
                val endpoint = endpoint(rdsInstance.id)

                if (endpoint == null) {
                    logger.info { "could not get endpoint for rds instance '${rdsInstance.name}'" }
                    statusManager.update(rdsInstance.id.id, Status.ERROR)
                } else {
                    if (HealthChecks.checkPort(
                            InetSocketAddress(
                                endpoint.endpoint.ipAddress,
                                endpoint.endpoint.agentPort
                            )
                        )
                    ) {
                        logger.info { "rds instance '${rdsInstance.name}' is healthy" }
                        statusManager.update(rdsInstance.id.id, Status.HEALTHY)
                    } else {
                        logger.info { "rds instance '${rdsInstance.name}' is unhealthy" }
                        statusManager.update(rdsInstance.id.id, Status.UNHEALTHY)
                    }
                }
            }
        }

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
        rdsScheduler.addRecurringTask(ensureTask)
    }

    fun endpoint(id: RdsInstanceId): AgentEndpoint? {
        val rdsInstance = repository.read(id.id) ?: return null

        val provider = providersManager.read(rdsInstance.provider) ?: return null
        val controller = controllersManager.readInternal(provider.controller) ?: return null

        val api = providersManager.createProviderApi(rdsInstance.provider) ?: return null
        val endpoint = api.endpoint(serverName(rdsInstance)) ?: return null

        return AgentEndpoint(
            endpoint = endpoint,
            caServerPublicKey = controller.caServerPublicKey,
            caClientPrivateKey = controller.caClientPrivateKey,
            caClientPublicKey = controller.caClientPublicKey
        )
    }

    fun read(id: UUID) = repository.read(id)?.let {
        RdsInstanceResponse(it.id.id, it.name, it.provider.id, statusManager.latest(it.id.id))
    }

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        RdsInstanceResponse(it.id.id, it.name, it.provider.id, statusManager.latest(it.id.id))
    }

    fun validate(request: RdsInstanceCreateRequest): MessagesResponse {

        if (repository.exists(request.name)) {
            return MessagesResponse.error(RdsInstanceCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        return MessagesResponse(emptyList())
    }

    fun create(request: RdsInstanceCreateRequest): RdsInstanceEntity? {

        val provider = providersManager.read(ProviderId(request.provider))
            ?: return null

        val controller = controllersManager.readInternal(provider.controller)
            ?: return null

        val serverKeyPair = Utils.createCertificate(controller.caServerPrivateKey, controller.caServerPublicKey)

        val entity = repository.create(
            request.provider, request.name,
            request.username,
            request.password,
            serverKeyPair.privateKey,
            serverKeyPair.publicKey,
        )

        scheduleApply(entity)

        return entity
    }

    private fun ensureAll() = repository.list().filter { diff(it) }.map {
        scheduleApply(it)
    }.all { it }

    private fun diff(rdsInstance: RdsInstanceEntity): Boolean {
        val api = providersManager.createProviderApi(rdsInstance.provider) ?: return false

        if (!api.hasServer(serverName(rdsInstance))) {
            return true
        }

        return false
    }

    fun createProviderApi(id: RdsInstanceId): HetznerApi? {
        val rdsInstance = repository.read(id.id) ?: return null
        return providersManager.createProviderApi(rdsInstance.provider)
    }

    private fun apply(rdsInstance: RdsInstanceEntity): Boolean {
        logger.info { "starting apply rds instance '${rdsInstance.name} (${rdsInstance.id})'" }

        if (statusManager.latest(rdsInstance.provider.id) != Status.HEALTHY) {
            logger.warn { "provider '${rdsInstance.provider}' for rds instance '${rdsInstance.name}' not healthy, skipping apply " }
            return false
        }

        val api = providersManager.createProviderApi(rdsInstance.provider) ?: return false

        val labels = HetznerLabels()
        labels.addLabel(Constants.rdsInstanceLabel, rdsInstance.id.toString())

        val data1VolumeName = data1VolumeName(rdsInstance)
        val backup1VolumeName = backup1VolumeName(rdsInstance)

        val serverName = serverName(rdsInstance)

        val sshKeyName = providersManager.sshKeyName(rdsInstance.provider) ?: run {
            logger.info { "could not find ssh key name for provider '${rdsInstance.provider}'" }
            return false
        }

        val data1VolumeResult = api.ensureVolume(data1VolumeName, labels)
        if (!data1VolumeResult) {
            logger.info {
                "could not create volume '$data1VolumeName' for instance '${rdsInstance.name}'"
            }
            return false
        }

        val backup1VolumeResult = api.ensureVolume(backup1VolumeName, labels)
        if (!backup1VolumeResult) {
            logger.info {
                "could not create volume '$backup1VolumeName' for instance '${rdsInstance.name}'"
            }
            return false
        }

        val provider = providersManager.read(rdsInstance.provider) ?: return false
        val controller = controllersManager.readInternal(provider.controller) ?: return false

        val cloudInit = CloudInitTemplates.solidblocksRdsCloudInit(
            solidblocksVersion(),
            api.getVolume(data1VolumeName)!!.linuxDevice,
            api.getVolume(backup1VolumeName)!!.linuxDevice,
            serverName,
            controller.caClientPublicKey,
            rdsInstance.serverPrivateKey,
            rdsInstance.serverPublicKey,
            "solidblocks-rds-postgresql-agent"
        )

        api.ensureServer(serverName, listOf(data1VolumeName, backup1VolumeName), cloudInit, sshKeyName, labels)
            ?: run {
                logger.info {
                    "could not ensure server '$serverName' for rds instance '${rdsInstance.name}'"
                }

                return false
            }

        statusManager.update(rdsInstance.id.id, Status.HEALTHY)

        return true
    }

    private fun scheduleApply(instance: RdsInstanceEntity): Boolean {
        logger.info { "scheduling apply for rds instance '${instance.name}'" }
        rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), instance))

        return true
    }
}
