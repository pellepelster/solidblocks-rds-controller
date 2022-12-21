package de.solidblocks.rds.controller.providers

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.MessagesResponse
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.instances.RdsInstancesRepository
import de.solidblocks.rds.controller.model.providers.ProviderEntity
import de.solidblocks.rds.controller.model.status.Status
import de.solidblocks.rds.controller.model.providers.ProvidersRepository
import de.solidblocks.rds.controller.model.status.StatusManager
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
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
    private val statusManager: StatusManager
) {

    private val logger = KotlinLogging.logger {}

    private var applyTask = Tasks.oneTime(
        "providers-apply-task", ProviderEntity::class.java
    ).execute { inst: TaskInstance<ProviderEntity>, ctx: ExecutionContext ->
        apply(inst.data)
    }

    private var healthcheckTask = Tasks.recurring("providers-healthcheck-task", FixedDelay.ofSeconds(15))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            for (provider in this.repository.list()) {

                val api = createProviderApi(provider.id)

                if (api == null) {
                    logger.info { "could not create api for provider '${provider.name}'" }
                    statusManager.update(provider.id, Status.ERROR)
                    continue
                }

                if (api.hasSSHKey(sshKeyName(provider))) {
                    logger.info { "provider '${provider.name}' is healthy" }
                    statusManager.update(provider.id, Status.HEALTHY)
                } else {
                    logger.info { "provider '${provider.name}' is unhealthy" }
                    statusManager.update(provider.id, Status.UNHEALTHY)
                    scheduleApplyTask(provider)
                }
            }
        }

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
    }

    fun createProviderApi(id: UUID): HetznerApi? {
        val provider = repository.read(id)

        if (provider == null) {
            logger.info { "could find provider '$id'" }
            return null
        }

        return HetznerApi(provider.apiKey)
    }

    fun sshKeyName(id: UUID) = repository.read(id)?.let { sshKeyName(it) }

    fun read(id: UUID) = repository.read(id)?.let {
        ProviderResponse(it.id, it.name, it.controller)
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
        ProviderResponse(it.id, it.name, it.controller)
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

        scheduleApplyTask(entity)

        return CreationResult(
            entity.let {
                ProviderResponse(it.id, it.name, it.controller)
            }
        )
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

    private fun diff(provider: ProviderEntity): Boolean {
        val api = HetznerApi(provider.apiKey)

        if (!api.hasSSHKey(sshKeyName(provider))) {
            return true
        }

        return false
    }

    private fun apply(provider: ProviderEntity): Boolean {
        logger.info { "starting work for provider '${provider.name} (${provider.id})'" }

        val api = createProviderApi(provider.id)

        if (api == null) {
            logger.info { "could not create api for provider '${provider.name} (${provider.id})'" }
            return false
        }

        if (!api.hasSSHKey(sshKeyName(provider))) {
            val response = api.ensureSSHKey(sshKeyName(provider), provider.sshPublicKey)

            if (!response) {
                logger.error {
                    "creating ssh key failed for provider '${provider.name}'"
                }

                return false
            }
        }

        return true
    }

    private fun scheduleApplyTask(provider: ProviderEntity): Boolean {
        logger.info { "scheduling apply for provider '${provider.name}'" }
        rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), provider))

        return true
    }
}
