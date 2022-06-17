package de.solidblocks.rds.controller.instances

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.cloudinit.CloudInitTemplates
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.instances.api.RdsInstanceCreateRequest
import de.solidblocks.rds.controller.instances.api.RdsInstanceResponse
import de.solidblocks.rds.controller.model.Constants.SERVER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SERVER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.instances.RdsInstanceEntity
import de.solidblocks.rds.controller.model.instances.RdsInstancesRepository
import de.solidblocks.rds.controller.model.providers.ProviderStatus
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.controller.utils.Constants
import de.solidblocks.rds.controller.utils.Constants.data1VolumeName
import de.solidblocks.rds.controller.utils.Constants.serverName
import de.solidblocks.rds.controller.utils.ErrorCodes
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.docker.HealthChecks
import de.solidblocks.rds.shared.SharedConstants
import de.solidblocks.rds.shared.solidblocksVersion
import mu.KotlinLogging
import java.net.InetSocketAddress
import java.util.*

class RdsInstancesManager(
    private val repository: RdsInstancesRepository,
    private val providersManager: ProvidersManager,
    private val controllersManager: ControllersManager,
    private val rdsScheduler: RdsScheduler
) {

    private var applyTask = Tasks.oneTime(
        "rds-instances-apply-task",
        RdsInstanceEntity::class.java
    )
        .execute { inst: TaskInstance<RdsInstanceEntity>, ctx: ExecutionContext ->
            apply(inst.data)
        }

    private var healthcheckTask = Tasks.recurring("rds-instances-healthcheck-task", FixedDelay.ofSeconds(15))
        .execute { inst: TaskInstance<Void>, ctx: ExecutionContext ->
            repository.list().forEach { rdsInstance ->

                val api = providersManager.createProviderApi(rdsInstance.provider)

                if (api == null) {
                    logger.info { "could not create api for rds instance '${rdsInstance.name}'" }
                    repository.updateStatus(rdsInstance.id, ProviderStatus.ERROR)
                } else {

                    val serverInfo = api.serverInfo(serverName(rdsInstance))

                    if (serverInfo == null) {
                        repository.updateStatus(rdsInstance.id, ProviderStatus.ERROR)
                    } else {

                        if (HealthChecks.checkPort(InetSocketAddress(serverInfo.ipAddress, serverInfo.agentPort))) {
                            logger.info { "rds instance '${rdsInstance.name}' is healthy" }
                            repository.updateStatus(rdsInstance.id, ProviderStatus.HEALTHY)
                        } else {
                            logger.info { "rds instance '${rdsInstance.name}' is unhealthy" }
                            repository.updateStatus(rdsInstance.id, ProviderStatus.UNHEALTHY)
                        }
                    }
                }
            }
        }

    private val logger = KotlinLogging.logger {}

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
    }

    fun read(id: UUID) = repository.read(id)?.let {
        RdsInstanceResponse(it.id, it.name, it.provider, it.status)
    }

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        RdsInstanceResponse(it.id, it.name, it.provider, it.status)
    }

    fun listInternal() = repository.list()

    fun validate(request: RdsInstanceCreateRequest): ValidationResult {

        if (repository.exists(request.name)) {
            return ValidationResult.error(RdsInstanceCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        return ValidationResult(emptyList())
    }

    fun create(request: RdsInstanceCreateRequest): CreationResult<RdsInstanceResponse> {

        val provider = providersManager.read(request.provider)
            ?: return CreationResult.error("provider '${request.provider}' not found")

        val controller = controllersManager.readInternal(provider.controller)
            ?: return CreationResult.error("controller '${provider.controller}' not found")

        val serverKeyPair = Utils.createCertificate(controller.caServerPrivateKey(), controller.caServerPublicKey())

        val entity = repository.create(
            request.provider, request.name,
            mapOf(
                SERVER_PRIVATE_KEY to serverKeyPair.privateKey,
                SERVER_PUBLIC_KEY to serverKeyPair.publicKey,
            )
        )

        scheduleApplyTask(entity)

        return CreationResult(
            entity.let {
                RdsInstanceResponse(it.id, it.name, it.provider, it.status)
            }
        )
    }

    fun applyAll(): Boolean {
        return listInternal().map {
            apply(it)
        }.any { it }
    }

    private fun apply(rdsInstance: RdsInstanceEntity): Boolean {
        logger.info { "starting work for rds instance '${rdsInstance.name} (${rdsInstance.id})'" }

        val hetznerApi = providersManager.createProviderApi(rdsInstance.provider) ?: run {
            logger.info { "could not provider api rds instance '${rdsInstance.id}'" }
            return false
        }

        val labels = HetznerLabels()
        labels.addLabel(Constants.rdsInstanceLabel, rdsInstance.id.toString())

        val volumeName = data1VolumeName(rdsInstance)
        val serverName = serverName(rdsInstance)

        val sshKeyName = providersManager.sshKeyName(rdsInstance.provider) ?: run {
            logger.info { "could not find ssh key name for provider '${rdsInstance.provider}'" }
            return false
        }

        val volumeResult = hetznerApi.ensureVolume(data1VolumeName(rdsInstance), labels)
        if (!volumeResult) {
            logger.info {
                "could not create volume '$volumeName' for instance '${rdsInstance.name}'"
            }
            return false
        }

        val provider = providersManager.read(rdsInstance.provider) ?: return false
        val controller = controllersManager.readInternal(provider.controller) ?: return false

        val cloudInit = CloudInitTemplates.solidblocksRdsCloudInit(
            solidblocksVersion(),
            "/dev/xyz",
            serverName,
            SharedConstants.githubUsername,
            SharedConstants.githubPat,
            controller.caClientPublicKey(),
            rdsInstance.serverPrivateKey(),
            rdsInstance.serverPublicKey(),
            "solidblocks-rds-postgresql-agent"
        )

        hetznerApi.ensureServer(serverName, volumeName, cloudInit, sshKeyName, labels) ?: run {
            logger.info {
                "could not server '$serverName' for rds instance '${rdsInstance.name}'"
            }

            return false
        }

        return true
    }

    fun scheduleApplyTask(instance: RdsInstanceEntity) {
        logger.info { "scheduling apply for rds instance '${instance.name}'" }
        rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), instance))
    }
}
