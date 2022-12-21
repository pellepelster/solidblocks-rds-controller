package de.solidblocks.rds.controller.instances

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.cloudinit.CloudInitTemplates
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.MessagesResponse
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.model.instances.RdsInstanceEntity
import de.solidblocks.rds.controller.model.instances.RdsInstancesRepository
import de.solidblocks.rds.controller.model.status.Status
import de.solidblocks.rds.controller.model.status.StatusManager
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.utils.Constants
import de.solidblocks.rds.controller.utils.Constants.backup1VolumeName
import de.solidblocks.rds.controller.utils.Constants.data1VolumeName
import de.solidblocks.rds.controller.utils.Constants.serverName
import de.solidblocks.rds.controller.utils.ErrorCodes
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.docker.HealthChecks
import de.solidblocks.rds.shared.dto.VersionResponse
import de.solidblocks.rds.shared.solidblocksVersion
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.*

data class RunningInstanceStatus(val status: String? = null)

data class RunningInstanceInfo(val instanceId: UUID, val ipAddress: String)

class RdsInstancesManager(
    private val repository: RdsInstancesRepository,
    private val providersManager: ProvidersManager,
    private val controllersManager: ControllersManager,
    private val rdsScheduler: RdsScheduler,
    private val statusManager: StatusManager
) {

    private val logger = KotlinLogging.logger {}

    private var instanceApplyTask = Tasks.oneTime(
        "rds-instances-apply-task", RdsInstanceEntity::class.java
    ).execute { inst: TaskInstance<RdsInstanceEntity>, ctx: ExecutionContext ->
        applyInstance(inst.data)
    }

    private var instanceConfigTask = Tasks.oneTime(
        "rds-instances-config-task", RdsInstanceEntity::class.java
    ).execute { inst: TaskInstance<RdsInstanceEntity>, ctx: ExecutionContext ->
        configureInstance(inst.data)
    }

    private var healthcheckTask = Tasks.recurring("rds-instances-healthcheck-task", FixedDelay.ofSeconds(15))
        .execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
            repository.list().forEach { rdsInstance ->

                val api = providersManager.createProviderApi(rdsInstance.provider)

                if (api == null) {
                    logger.info { "could not create api for rds instance '${rdsInstance.name}'" }
                    statusManager.update(rdsInstance.id, Status.ERROR)
                } else {

                    val serverInfo = api.serverInfo(serverName(rdsInstance))

                    if (serverInfo == null) {
                        statusManager.update(rdsInstance.id, Status.ERROR)
                    } else {

                        if (HealthChecks.checkPort(InetSocketAddress(serverInfo.ipAddress, serverInfo.agentPort))) {
                            logger.info { "rds instance '${rdsInstance.name}' is healthy" }
                            statusManager.update(rdsInstance.id, Status.HEALTHY)
                        } else {
                            logger.info { "rds instance '${rdsInstance.name}' is unhealthy" }
                            statusManager.update(rdsInstance.id, Status.UNHEALTHY)
                        }
                    }
                }
            }
        }

    init {
        rdsScheduler.addOneTimeTask(instanceApplyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
    }

    fun read(id: UUID) = repository.read(id)?.let {
        RdsInstanceResponse(it.id, it.name, it.provider)
    }

    fun readInternal(id: UUID) = repository.read(id)

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        RdsInstanceResponse(it.id, it.name, it.provider)
    }

    fun validate(request: RdsInstanceCreateRequest): MessagesResponse {

        if (repository.exists(request.name)) {
            return MessagesResponse.error(RdsInstanceCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        return MessagesResponse(emptyList())
    }

    fun create(request: RdsInstanceCreateRequest): CreationResult<RdsInstanceResponse> {

        val provider = providersManager.read(request.provider)
            ?: return CreationResult.error("provider '${request.provider}' not found")

        val controller = controllersManager.readInternal(provider.controller)
            ?: return CreationResult.error("controller '${provider.controller}' not found")

        val serverKeyPair = Utils.createCertificate(controller.caServerPrivateKey, controller.caServerPublicKey)

        val entity = repository.create(
            request.provider, request.name,
            request.username,
            request.password,
            serverKeyPair.privateKey,
            serverKeyPair.publicKey,
        )

        scheduleApplyTask(entity)

        return CreationResult(
            entity.let {
                RdsInstanceResponse(it.id, it.name, it.provider)
            }
        )
    }

    fun runningInstances(): List<RunningInstanceInfo> {
        return repository.list().mapNotNull {
            runningInstanceInfo(it.id)
        }
    }

    fun runningInstanceInfo(id: UUID): RunningInstanceInfo? {
        val rdsInstance = repository.read(id) ?: return null

        val hetznerApi = providersManager.createProviderApi(rdsInstance.provider) ?: run {
            logger.info { "could not create provider instance for rds instance '${rdsInstance.id}'" }
            return null
        }

        val server = hetznerApi.getServer(serverName(rdsInstance)) ?: return null

        return RunningInstanceInfo(rdsInstance.id, server.publicNet.ipv4.ip)
    }

    fun runningInstancesClients() = runningInstances().mapNotNull {
        createHttpClient(it.instanceId)
    }

    private fun createHttpClient(id: UUID): MtlsHttpClient? {
        val instance = read(id) ?: return null
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
                scheduleApplyTask(it)
            } else {
                true
            }
        }.all { it }
    }

    private fun diff(rdsInstance: RdsInstanceEntity): Boolean {
        val api = providersManager.createProviderApi(rdsInstance.provider) ?: return false

        if (!api.hasServer(serverName(rdsInstance))) {
            return true
        }

        return false
    }

    private fun applyInstance(rdsInstance: RdsInstanceEntity): Boolean {
        logger.info { "starting work for rds instance '${rdsInstance.name} (${rdsInstance.id})'" }

        val hetznerApi = providersManager.createProviderApi(rdsInstance.provider) ?: run {
            logger.info { "could not provider api rds instance '${rdsInstance.id}'" }
            return false
        }

        val labels = HetznerLabels()
        labels.addLabel(Constants.rdsInstanceLabel, rdsInstance.id.toString())

        val data1VolumeName = data1VolumeName(rdsInstance)
        val backup1VolumeName = backup1VolumeName(rdsInstance)

        val serverName = serverName(rdsInstance)

        val sshKeyName = providersManager.sshKeyName(rdsInstance.provider) ?: run {
            logger.info { "could not find ssh key name for provider '${rdsInstance.provider}'" }
            return false
        }

        val data1VolumeResult = hetznerApi.ensureVolume(data1VolumeName, labels)
        if (!data1VolumeResult) {
            logger.info {
                "could not create volume '$data1VolumeName' for instance '${rdsInstance.name}'"
            }
            return false
        }

        val backup1VolumeResult = hetznerApi.ensureVolume(backup1VolumeName, labels)
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
            hetznerApi.getVolume(data1VolumeName)!!.linuxDevice,
            hetznerApi.getVolume(backup1VolumeName)!!.linuxDevice,
            serverName,
            controller.caClientPublicKey,
            rdsInstance.serverPrivateKey,
            rdsInstance.serverPublicKey,
            "solidblocks-rds-postgresql-agent"
        )

        hetznerApi.ensureServer(serverName, listOf(data1VolumeName, backup1VolumeName), cloudInit, sshKeyName, labels)
            ?: run {
                logger.info {
                    "could not ensure server '$serverName' for rds instance '${rdsInstance.name}'"
                }

                return false
            }

        scheduleConfigureTask(rdsInstance)
        return true
    }

    private fun configureInstance(rdsInstance: RdsInstanceEntity): Boolean {

        val client = createHttpClient(rdsInstance.id) ?: return false

        return true
    }

    private fun scheduleApplyTask(instance: RdsInstanceEntity): Boolean {
        logger.info { "scheduling apply for rds instance '${instance.name}'" }
        rdsScheduler.scheduleTask(instanceApplyTask.instance(UUID.randomUUID().toString(), instance))

        return true
    }

    private fun scheduleConfigureTask(instance: RdsInstanceEntity): Boolean {
        logger.info { "scheduling configure for rds instance '${instance.name}'" }
        rdsScheduler.scheduleTask(instanceConfigTask.instance(UUID.randomUUID().toString(), instance))
        return true
    }
}
