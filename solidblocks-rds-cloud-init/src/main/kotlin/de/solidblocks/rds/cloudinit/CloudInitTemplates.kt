package de.solidblocks.rds.cloudinit

import de.solidblocks.rds.shared.Templates
import java.nio.file.Path


class CloudInitTemplates {

    fun solidblocksRdsCloudInit(
        targetDir: Path,
        solidblocksVersion: String,
        storageLocalDevice: String,
        hostname: String
    ) {

        Templates.writeTemplate(
            "solidblocks-rds-cloud-init.sh",
            targetDir.resolve("solidblocks-rds-cloud-init.sh"),
            variables = mapOf(
                "solidblocks_version" to solidblocksVersion,
                "solidblocks_hostname" to hostname,
                "storage_local_device" to storageLocalDevice
            ),
            basePackagePath = "/templates-generated"
        )
    }

}
