package de.solidblocks.rds.controller.instances

import de.solidblocks.rds.agent.MtlsHttpClient
import de.solidblocks.rds.controller.controllers.ControllersManager
import de.solidblocks.rds.controller.model.providers.ProviderEntity
import de.solidblocks.rds.controller.providers.HetznerApi
import de.solidblocks.rds.controller.providers.ProvidersManager
import de.solidblocks.rds.shared.VersionResponse
import mu.KotlinLogging
import java.util.*

class RdsInstancesWorker(
    private val rdsInstancesManager: RdsInstancesManager,
    private val providersManager: ProvidersManager,
    private val controllersManager: ControllersManager
) {

    private val logger = KotlinLogging.logger {}

    /*
    private var provisionerApplyTask = Tasks.recurring("provisioner-apply-task", FixedDelay.ofSeconds(30))
        .execute { _: TaskInstance<Void>, _: ExecutionContext ->

            executor.executeWithLock<Any>(
                {
                    providersManager.apply()
                },
                LockConfiguration(
                    Instant.now(), "global-apply-task", Duration.ofSeconds(60), Duration.ofSeconds(5)
                )
            )
        }
    */

    fun apply(hetznerApi: HetznerApi, provider: ProviderEntity): Boolean {

        /*
        val instances = repository.list(provider.id)

        if (!hetznerApi.cleanupServersNotInList(instances)) {
            logger.error {
                "cleaning up deleted servers failed"
            }

            return false
        }

        return instances.map {
            logger.info {
                "applying config for instance '${it.name}'"
            }

            hetznerApi.ensureVolume(Constants.data1VolumeName(it))
            hetznerApi.ensureServer(
                Constants.serverName(it),
                Constants.data1VolumeName(it), "",
                Constants.sshKeyName(provider)
            )

            true
        }.all { it }
         */
        return true
    }

    data class RunningInstanceInfo(val instanceId: UUID, val ipAddress: String)

    fun runningInstances(): List<RunningInstanceInfo> {
        return rdsInstancesManager.listInternal().map { rdsInstance ->
            val hetznerApi = providersManager.createProviderApi(rdsInstance.provider) ?: run {
                logger.info { "could not create provider instance for rds instance '${rdsInstance.id}'" }
                return@map null
            }

            return@map rdsInstance.id to hetznerApi
        }.filterNotNull().flatMap {
            it.second.allManagedServers().map { server ->
                RunningInstanceInfo(it.first, server.publicNet.ipv4.ip)
            }
        }
    }

    data class RunningInstanceStatus(val status: String? = null)

    fun runningInstancesStatus() = runningInstances().map {

        val instance = rdsInstancesManager.read(it.instanceId) ?: return@map RunningInstanceStatus()
        val provider = providersManager.read(instance.provider) ?: return@map RunningInstanceStatus()
        val controller = controllersManager.readInternal(provider.controller) ?: return@map RunningInstanceStatus()

        rdsInstancesManager.read(it.instanceId)

        val client = MtlsHttpClient(
            "https://${it.ipAddress}:8080",
            controller.caServerPublicKey(),
            controller.caClientPrivateKey(),
            controller.caClientPublicKey()
        )

        try {
            val version = client.get<VersionResponse>("/v1/agent/version")
            if (version.isSuccessful) {
                return@map RunningInstanceStatus(version.data!!.version)
            }
        } catch (e: Exception) {
            return@map RunningInstanceStatus()
        }

        return@map RunningInstanceStatus()
    }
}
