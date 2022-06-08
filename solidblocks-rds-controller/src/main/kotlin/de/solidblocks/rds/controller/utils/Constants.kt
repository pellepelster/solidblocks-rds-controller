package de.solidblocks.rds.controller.utils

import de.solidblocks.rds.controller.model.ProviderEntity
import de.solidblocks.rds.controller.model.RdsInstanceEntity

object Constants {
    fun data1VolumeName(rdsInstance: RdsInstanceEntity) = "${rdsInstance.name}-data1"
    fun serverName(rdsInstance: RdsInstanceEntity) = "${rdsInstance.name}"
    fun sshKeyName(provider: ProviderEntity) = provider.name

    val labelNamespace: String = "solidblocks.de"

    val managedByLabel: String = "$labelNamespace/managed"

    val versionLabel: String = "$labelNamespace/version"
}
