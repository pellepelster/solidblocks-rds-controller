package de.solidblocks.rds.cloudinit

import de.solidblocks.rds.shared.Templates
import java.nio.file.Path

class CloudInitTemplates {

    fun solidblocksRdsCloudInit(targetDir: Path) {
        Templates.writeTemplate(
            "solidblocks-rds-cloud-init.sh",
            targetDir.resolve("solidblocks-rds-cloud-init.sh"),
            basePackagePath = "/lib-cloud-init-generated"
        )
    }

}
