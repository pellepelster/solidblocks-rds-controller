package de.solidblocks.rds.cloudinit

import de.solidblocks.rds.shared.Templates
import java.nio.file.Path

class CloudInitTemplates {

    companion object {
        fun solidblocksRdsCloudInit(
            targetFile: Path,
            solidblocksVersion: String,
            storageLocalDevice: String,
            hostname: String
        ) {

            Templates.writeTemplate(
                "solidblocks-rds-cloud-init.sh",
                targetFile,
                variables = mapOf(
                    "solidblocks_version" to solidblocksVersion,
                    "solidblocks_hostname" to hostname,
                    "storage_local_device" to storageLocalDevice
                ),
                basePackagePath = "/templates-generated"
            )
        }
    }
}
