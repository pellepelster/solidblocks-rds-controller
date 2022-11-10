package de.solidblocks.rds.cloudinit

import de.solidblocks.rds.shared.Templates
import java.util.Base64.getEncoder

class CloudInitTemplates {

    companion object {
        fun solidblocksRdsCloudInit(
            solidblocksVersion: String,
            data1Device: String,
            backup1Device: String,
            hostname: String,
            clientCAPublicKey: String,
            serverPrivateKey: String,
            serverPublicKey: String,
            solidblocksAgent: String

        ) = Templates.writeTemplate(
            "solidblocks-rds-cloud-init.sh",
            variables = mapOf(
                "solidblocks_version" to solidblocksVersion,
                "solidblocks_storage_data1_device" to data1Device,
                "solidblocks_storage_backup1_device" to backup1Device,
                "solidblocks_hostname" to hostname,
                "solidblocks_agent" to solidblocksAgent,
                "solidblocks_client_ca_public_key" to getEncoder().encodeToString(clientCAPublicKey.toByteArray()),
                "solidblocks_server_private_key" to getEncoder().encodeToString(serverPrivateKey.toByteArray()),
                "solidblocks_server_public_key" to getEncoder().encodeToString(serverPublicKey.toByteArray()),
            ),
            basePackagePath = "/templates-generated"
        )
    }
}
