package de.solidblocks.rds.controller.utils

import de.solidblocks.rds.controller.model.LOCATION
import de.solidblocks.rds.controller.model.instances.RdsInstanceEntity
import de.solidblocks.rds.controller.model.providers.ProviderEntity

object Constants {

    fun data1VolumeName(rdsInstance: RdsInstanceEntity) =
        "${rdsInstance.name}-${LOCATION.fsn1}-data1-${rdsInstance.id}".lowercase()

    fun backup1VolumeName(rdsInstance: RdsInstanceEntity) =
        "${rdsInstance.name}-${LOCATION.fsn1}-backup1-${rdsInstance.id}".lowercase()

    fun serverName(rdsInstance: RdsInstanceEntity) =
        "${rdsInstance.name}-${LOCATION.fsn1}-${rdsInstance.id}".lowercase()

    fun sshKeyName(provider: ProviderEntity) = "${provider.name}-${provider.id}".lowercase()

    val labelNamespace: String = "solidblocks.de"

    val managedByLabel: String = "$labelNamespace/managed"

    val versionLabel: String = "$labelNamespace/version"

    val rdsInstanceLabel: String = "$labelNamespace/rdsInstance"

    val cloudInitChecksumLabel: String = "$labelNamespace/cloudInitChecksum"
}
