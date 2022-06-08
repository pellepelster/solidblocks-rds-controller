package de.solidblocks.rds.controller.providers

import de.solidblocks.rds.controller.model.RdsInstanceEntity
import de.solidblocks.rds.controller.utils.Constants.cloudInitChecksumLabel
import de.solidblocks.rds.controller.utils.Constants.labelNamespace
import de.solidblocks.rds.controller.utils.Constants.managedByLabel
import de.solidblocks.rds.controller.utils.Constants.versionLabel
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.controller.utils.Waiter
import de.solidblocks.rds.shared.solidblocksVersion
import me.tomsdevsn.hetznercloud.HetznerCloudAPI
import me.tomsdevsn.hetznercloud.objects.general.Action
import me.tomsdevsn.hetznercloud.objects.general.Server
import me.tomsdevsn.hetznercloud.objects.general.Volume
import me.tomsdevsn.hetznercloud.objects.request.AttachVolumeRequest
import me.tomsdevsn.hetznercloud.objects.request.SSHKeyRequest
import me.tomsdevsn.hetznercloud.objects.request.ServerRequest
import me.tomsdevsn.hetznercloud.objects.request.VolumeRequest
import mu.KotlinLogging

class HetznerApi(apiToken: String) {

    private val logger = KotlinLogging.logger {}

    private val hetznerCloudAPI = HetznerCloudAPI(apiToken)

    fun deleteAllServers(): Boolean {
        return allManagedServers().map {
            logger.info { "deleting server '${it.name}'" }

            try {
                hetznerCloudAPI.deleteServer(it.id)
                true
            } catch (e: Exception) {
                logger.error(e) { "failed to delete server '${it.name}'" }
                false
            }
        }.all { it }
    }

    fun deleteAllVolumes(): Boolean {
        return hetznerCloudAPI.volumes.volumes.map {
            logger.info { "deleting volume '${it.name}'" }

            if (it.server != null) {
                val response = hetznerCloudAPI.detachVolume(it.id)

                if (!waitForVolumeAction(it.id, response.action)) {
                    return@map false
                }
            }

            try {
                hetznerCloudAPI.deleteVolume(it.id)
                true
            } catch (e: Exception) {
                logger.error(e) { "failed to delete volume '${it.name}'" }
                false
            }
        }.all { it }
    }

    fun deleteAllSSHKeys(): Boolean {
        return hetznerCloudAPI.sshKeys.sshKeys.map {
            logger.info { "deleting ssh key '${it.name}'" }

            try {
                hetznerCloudAPI.deleteSSHKey(it.id)
                true
            } catch (e: Exception) {
                logger.error(e) { "failed to delete ssh key '${it.name}'" }
                false
            }
        }.all { it }
    }

    fun hasVolume(name: String) = getVolume(name) != null

    fun getVolume(name: String) = hetznerCloudAPI.volumes.volumes.firstOrNull { it.name == name }

    fun ensureVolume(name: String): Boolean {

        if (hasVolume(name)) {
            return true
        }

        logger.info { "creating volume '$name'" }
        val response = hetznerCloudAPI.createVolume(
            VolumeRequest.builder().location("nbg1").name(name).size(16).format("xfs").build()
        )

        return waitForVolumeAction(response.volume, response.action)
    }

    private fun waitForVolumeAction(volume: Volume, action: Action) = Waiter.defaultWaiter().waitFor {
        val actionResult = hetznerCloudAPI.getActionOfVolume(volume.id, action.id)
        logger.info { "waiting for volume '${actionResult.action.command}' to finish for volume '${volume.name}', current status is '${actionResult.action.status}'" }
        actionResult.action.finished != null && actionResult.action.status == "success"
    }

    private fun waitForVolumeAction(volumeId: Long, action: Action) = Waiter.defaultWaiter().waitFor {
        val actionResult = hetznerCloudAPI.getActionOfVolume(volumeId, action.id)
        logger.info { "waiting for volume '${actionResult.action.command}' to finish for volume '$volumeId', current status is '${actionResult.action.status}'" }
        actionResult.action.finished != null && actionResult.action.status == "success"
    }

    fun hasServer(name: String) = getServer(name) != null

    fun getServer(name: String) = allManagedServers().firstOrNull { it.name == name }

    fun allManagedServers() = hetznerCloudAPI.servers.servers.filter { it.labels[managedByLabel] == true.toString() }

    fun cleanupServersNotInList(instances: List<RdsInstanceEntity>): Boolean {
        logger.info { "cleaning up deleted servers" }

        return allManagedServers().map { server ->

            if (instances.none { server.name == it.name }) {
                logger.info { "removing server '${server.name}'" }
                if (!deleteServer(server.name)) {
                    return@map false
                }
            }

            true
        }.all { it }
    }

    fun deleteServer(name: String): Boolean {
        val server = getServer(name)

        if (server == null) {
            logger.error { "server '$name' not found" }
            return false
        }

        logger.info { "deleting server '$name'" }
        val response = hetznerCloudAPI.deleteServer(server.id)

        return waitForServerAction(server, response.action)
    }

    fun ensureServer(serverName: String, volumeName: String, userData: String, sshKeyName: String): Boolean {

        val volume = getVolume(volumeName)
        if (volume == null) {
            logger.error { "volume '$volumeName' not found" }
            return false
        }

        val sshKey = getSSHKey(sshKeyName)
        if (sshKey == null) {
            logger.error { "ssh key '$sshKeyName' not found" }
            return false
        }

        val additionalSSHKeys = hetznerCloudAPI.sshKeys.sshKeys.filter { it.id == sshKey.id }

        var server = getServer(serverName)

        if (server == null) {
            logger.info { "creating server '$serverName'" }

            val labels = HetznerLabels(labelNamespace)
            labels.addLabel(managedByLabel, "true")
            labels.addLabel(versionLabel, solidblocksVersion())
            labels.addLabel(cloudInitChecksumLabel, solidblocksVersion())

            val response = hetznerCloudAPI.createServer(
                ServerRequest.builder()
                    .location("nbg1")
                    .image("debian-10")
                    .sshKey(sshKey.name)
                    .sshKeys(additionalSSHKeys.map { it.name })
                    .userData(userData)
                    .startAfterCreate(false)
                    .labels(labels.labels())
                    .serverType("cx11")
                    .name(serverName).build()
            )

            waitForServerAction(response.server, response.action)
            server = response.server
        }

        if (server == null) {
            logger.error { "failed to ensure server '$serverName'" }
            return false
        }

        if (server.volumes.any { it == volume.id }) {
            return true
        }

        val response =
            hetznerCloudAPI.attachVolumeToServer(volume.id, AttachVolumeRequest.builder().serverID(server.id).build())
        return waitForServerAction(server, response.action)
    }

    private fun Action.succesfull(): Boolean = this.finished != null && this.status == "success"

    fun waitForServerAction(server: Server, action: Action) = Waiter.defaultWaiter().waitFor {

        if (action.succesfull()) {
            return@waitFor true
        }

        val actionResult = hetznerCloudAPI.getActionOfServer(server.id, action.id)
        logger.info { "waiting for action '${actionResult.action.command}' to finish for server '${server.name}', current status is '${actionResult.action.status}'" }
        actionResult.action.succesfull()
    }

    fun hasSSHKey(name: String) = getSSHKey(name) != null

    fun getSSHKey(name: String) = hetznerCloudAPI.sshKeys.sshKeys.firstOrNull { it.name == name }

    fun ensureSSHKey(name: String, publicKey: String): Boolean {

        if (hasSSHKey(name)) {
            // TODO update public key if needed
            return true
        }

        logger.info { "creating ssh key '$name'" }
        val response = hetznerCloudAPI.createSSHKey(SSHKeyRequest.builder().name(name).publicKey(publicKey).build())

        logger.info { "created ssh key '$name' with fingerprint '${response.sshKey.fingerprint}'" }

        return response.sshKey != null
    }
}
