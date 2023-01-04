package de.solidblocks.rds.controller.providers

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay.ofSeconds
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.MessagesResponse
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.log.LogManager
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.entities.ProviderEntity
import de.solidblocks.rds.controller.model.entities.ProviderId
import de.solidblocks.rds.controller.model.repositories.LogRepository
import de.solidblocks.rds.controller.model.repositories.ProvidersRepository
import de.solidblocks.rds.controller.model.repositories.RdsInstancesRepository
import de.solidblocks.rds.controller.model.status.HealthStatus
import de.solidblocks.rds.controller.model.status.ProvisioningStatus.*
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
import de.solidblocks.rds.controller.status.StatusManager
import de.solidblocks.rds.controller.utils.Constants.sshKeyName
import de.solidblocks.rds.controller.utils.ErrorCodes
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import java.util.*

class ProvidersManager(
    private val repository: ProvidersRepository,
    private val rdsInstancesRepository: RdsInstancesRepository,
    private val controllersManager: ControllersManager,
    private val rdsScheduler: RdsScheduler,
    private val statusManager: StatusManager,
    private val logRepository: LogRepository,
) {

    private val logger = KotlinLogging.logger {}

    private var applyTask = Tasks.oneTime(
        "providers-apply-task", ProviderEntity::class.java
    ).execute { inst: TaskInstance<ProviderEntity>, _: ExecutionContext ->
        with(inst.data) {
            try {
                statusManager.update(id, RUNNING)
                apply(inst.data, LogManager(id.id, logRepository, logger))
                statusManager.update(id, FINISHED)
            } catch (e: Exception) {
                statusManager.update(id, FAILED)
                logger.error(e) { "failed to execute apply task" }
            }
        }
    }

    private var ensureTask =
        Tasks.recurring("providers-ensure-task", ofSeconds(60)).execute { _: TaskInstance<Void>, _: ExecutionContext ->
            ensureAll()
        }

    private var healthcheckTask = Tasks.recurring("providers-healthcheck-task", ofSeconds(15))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            for (provider in this.repository.list()) {
                val api = createProviderApi(provider.id) ?: continue

                if (api.hasSSHKey(sshKeyName(provider))) {
                    logger.info { "provider '${provider.name}' is healthy" }
                    statusManager.update(provider.id.id, HealthStatus.HEALTHY)
                } else {
                    logger.info { "provider '${provider.name}' is unhealthy" }
                    statusManager.update(provider.id.id, HealthStatus.UNHEALTHY)
                }
            }
        }

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
        rdsScheduler.addRecurringTask(ensureTask)
    }

    fun createProviderApi(id: ProviderId): HetznerApi? {
        val provider = repository.read(id.id)

        if (provider == null) {
            logger.error { "error creating provider api, could not find provider '$id'" }
            return null
        }

        return HetznerApi(provider.apiKey)
    }

    fun read(id: ProviderId) = repository.read(id.id)?.let {
        ProviderResponse(it.id.id, it.name, it.controller.id, statusManager.latest(id.id))
    }

    fun delete(id: UUID): MessagesResponse {

        if (rdsInstancesRepository.list(id).isNotEmpty()) {
            return MessagesResponse.error(ErrorCodes.PROVIDER_NOT_EMPTY)
        }

        if (repository.delete(id)) {
            return MessagesResponse.ok()
        }

        return MessagesResponse.error(ErrorCodes.DELETE_FAILED)
    }

    fun list() = repository.list().map {
        ProviderResponse(it.id.id, it.name, it.controller.id, statusManager.latest(it.id.id))
    }

    fun validate(request: ProviderCreateRequest): MessagesResponse {

        if (repository.exists(request.name)) {
            return MessagesResponse.error(ProviderCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        try {
            val cloudApi = HetznerCloudAPI(request.apiKey)
            cloudApi.datacenters.datacenters
        } catch (e: Exception) {
            return MessagesResponse.error(ProviderCreateRequest::apiKey, ErrorCodes.INVALID)
        }

        return MessagesResponse(emptyList())
    }

    fun create(request: ProviderCreateRequest): CreationResult<ProviderResponse> {

        val sshKey = Utils.generateSshKey(request.name)

        val entity = repository.create(
            request.name, controllersManager.defaultController(),
            mapOf(
                API_KEY to request.apiKey, SSH_PUBLIC_KEY to sshKey.publicKey, SSH_PRIVATE_KEY to sshKey.privateKey
            )
        )

        scheduleApply(entity)

        return CreationResult(
            entity.let {
                ProviderResponse(it.id.id, it.name, it.controller.id, statusManager.latest(it.id.id))
            }
        )
    }

    fun sshKeyName(id: ProviderId) = repository.read(id.id)?.let { sshKeyName(it) }

    fun ensureAll() = repository.list().filter { diff(it) }.map {
        scheduleApply(it)
    }.all { it }

    private fun diff(provider: ProviderEntity): Boolean {
        val api = HetznerApi(provider.apiKey)

        if (!api.hasSSHKey(sshKeyName(provider))) {
            return true
        }

        return false
    }

    private fun apply(provider: ProviderEntity, logManager: LogManager): Boolean {
        logManager.info { "starting provisioning for provider '${provider.name}'" }

        val api = createProviderApi(provider.id) ?: return false

        if (!api.hasSSHKey(sshKeyName(provider))) {
            if (!api.ensureSSHKey(sshKeyName(provider), provider.sshPublicKey)) {
                logManager.error {
                    "creating ssh key failed for provider '${provider.name}'"
                }

                return false
            }

            logManager.error {
                "created ssh key for provider '${provider.name}'"
            }
        }

        logManager.info { "provisioning finished for provider '${provider.name}'" }

        return true
    }

    private fun scheduleApply(provider: ProviderEntity): Boolean {
        logger.info { "scheduling apply for provider '${provider.name}'" }
        rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), provider))
        return true
    }
}
