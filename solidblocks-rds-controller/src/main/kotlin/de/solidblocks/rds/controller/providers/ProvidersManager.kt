package de.solidblocks.rds.controller.providers

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import de.solidblocks.rds.base.Utils
import de.solidblocks.rds.controller.RdsScheduler
import de.solidblocks.rds.controller.api.CreationResult
import de.solidblocks.rds.controller.api.ValidationResult
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.model.Constants.API_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.SSH_PUBLIC_KEY
import de.solidblocks.rds.controller.model.providers.ProviderEntity
import de.solidblocks.rds.controller.model.providers.ProviderStatus
import de.solidblocks.rds.controller.model.providers.ProvidersRepository
import de.solidblocks.rds.controller.providers.api.ProviderCreateRequest
import de.solidblocks.rds.controller.providers.api.ProviderResponse
import de.solidblocks.rds.controller.utils.Constants
import de.solidblocks.rds.controller.utils.ErrorCodes
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import mu.KotlinLogging
import java.util.*

class ProvidersManager(
        private val repository: ProvidersRepository,
        private val controllersManager: ControllersManager,
        private val rdsScheduler: RdsScheduler
) {

    private val logger = KotlinLogging.logger {}

    private var applyTask = Tasks.oneTime(
            "providers-apply-task",
            ProviderEntity::class.java
    )
            .execute { inst: TaskInstance<ProviderEntity>, ctx: ExecutionContext ->
                apply(inst.data)
            }

    private var healthcheckTask = Tasks.recurring("providers-healthcheck-task", FixedDelay.ofSeconds(15))
            .execute { _: TaskInstance<Void>, _: ExecutionContext ->

                for (provider in this.repository.list()) {

                    val api = createProviderApi(provider.id)
                    if (api == null) {
                        logger.info { "could not create api for provider '${provider.name}'" }
                        repository.updateStatus(provider.id, ProviderStatus.ERROR)
                        continue
                    }

                    if (api.hasSSHKey(Constants.sshKeyName(provider))) {
                        logger.info { "provider '${provider.name}' is healthy" }
                        repository.updateStatus(provider.id, ProviderStatus.HEALTHY)
                    } else {
                        logger.info { "provider '${provider.name}' is unhealthy" }
                        repository.updateStatus(provider.id, ProviderStatus.UNHEALTHY)
                        scheduleApplyTask(provider)
                    }
                }
            }

    init {
        rdsScheduler.addOneTimeTask(applyTask)
        rdsScheduler.addRecurringTask(healthcheckTask)
    }

    fun read(id: UUID) = repository.read(id)?.let {
        ProviderResponse(it.id, it.name, it.controller, it.status)
    }

    fun delete(id: UUID) = repository.delete(id)

    fun list() = repository.list().map {
        ProviderResponse(it.id, it.name, it.controller, it.status)
    }

    fun listInternal() = repository.list()

    fun validate(request: ProviderCreateRequest): ValidationResult {

        if (repository.exists(request.name)) {
            return ValidationResult.error(ProviderCreateRequest::name, ErrorCodes.DUPLICATE)
        }

        try {
            val cloudApi = HetznerCloudAPI(request.apiKey)
            cloudApi.datacenters.datacenters
        } catch (e: Exception) {
            return ValidationResult.error(ProviderCreateRequest::apiKey, ErrorCodes.INVALID)
        }

        return ValidationResult(emptyList())
    }

    fun create(request: ProviderCreateRequest): CreationResult<ProviderResponse> {
        val sshKey = Utils.generateSshKey(request.name)

        val entity = repository.create(
                request.name,
                controllersManager.defaultController(),
                mapOf(
                        API_KEY to request.apiKey,
                        SSH_PUBLIC_KEY to sshKey.publicKey,
                        SSH_PRIVATE_KEY to sshKey.privateKey
                )
        )

        scheduleApplyTask(entity)

        return CreationResult(
                entity.let {
                    ProviderResponse(it.id, it.name, it.controller, it.status)
                }
        )
    }

    fun applyAll(): Boolean {
        return listInternal().map {
            apply(it)
        }.all { it }
    }

    fun createProviderApi(id: UUID) = repository.read(id)?.let { HetznerApi(it.apiKey()) }

    fun sshKeyName(id: UUID) = repository.read(id)?.let { Constants.sshKeyName(it) }

    private fun apply(provider: ProviderEntity): Boolean {
        logger.info { "starting work for provider '${provider.name} (${provider.id})'" }

        val api = createProviderApi(provider.id)

        if (api == null) {
            logger.info { "could not create api for provider '${provider.name} (${provider.id})'" }
            return false
        }

        val response = api.ensureSSHKey(Constants.sshKeyName(provider), provider.sshPublicKey())

        if (!response) {
            logger.error {
                "creating ssh key failed for provider '${provider.name}'"
            }

            return false
        }

        return true
    }

    private fun scheduleApplyTask(provider: ProviderEntity) {
        logger.info { "scheduling apply for provider '${provider.name}'" }
        rdsScheduler.scheduleTask(applyTask.instance(UUID.randomUUID().toString(), provider))
    }
}
