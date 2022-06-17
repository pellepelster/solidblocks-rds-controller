package de.solidblocks.rds.cloudinit

import de.solidblocks.rds.shared.Templates
import java.util.Base64.*

class CloudInitTemplates {

    companion object {
        fun solidblocksRdsCloudInit(
            solidblocksVersion: String,
            storageLocalDevice: String,
            hostname: String,
            githubUsername: String,
            githubPat: String,
            clientCAPublicKey: String,
            serverPrivateKey: String,
            serverPublicKey: String,
            solidblocksAgent: String

        ) = Templates.writeTemplate(
            "solidblocks-rds-cloud-init.sh",
            variables = mapOf(
                "solidblocks_version" to solidblocksVersion,
                "storage_local_device" to storageLocalDevice,
                "solidblocks_hostname" to hostname,
                "github_username" to githubUsername,
                "github_pat" to githubPat,
                "solidblocks_agent" to solidblocksAgent,
                "solidblocks_client_ca_public_key" to getEncoder().encodeToString(clientCAPublicKey.toByteArray()),
                "solidblocks_server_private_key" to getEncoder().encodeToString(serverPrivateKey.toByteArray()),
                "solidblocks_server_public_key" to getEncoder().encodeToString(serverPublicKey.toByteArray()),
            ),
            basePackagePath = "/templates-generated"
        )
    }
}
