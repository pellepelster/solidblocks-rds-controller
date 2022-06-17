package de.solidblocks.rds.agent

import de.solidblocks.rds.shared.AGENT_BASE_PATH
import de.solidblocks.rds.shared.VersionResponse
import mu.KotlinLogging

class BaseAgentApiClient(
    val address: String,
    caCertificateRaw: String,
    privateKeyRaw: String,
    publicKeyRaw: String
) {

    private val logger = KotlinLogging.logger {}

    private val client = MtlsHttpClient(address, caCertificateRaw, privateKeyRaw, publicKeyRaw)

    fun version() = try {
        val currentVersion: HttpResponse<VersionResponse> = client.get("$AGENT_BASE_PATH/version")
        currentVersion.data
    } catch (e: Exception) {
        // logger.error(e) { "error executing request for '$address'" }
        null
    }

    fun triggerUpdate(targetVersion: String): Boolean? {
        val currentVersion: HttpResponse<TriggerUpdateResponse> = client.post(
            "$AGENT_BASE_PATH/${TriggerUpdateRequest.TRIGGER_UPDATE_PATH}",
            TriggerUpdateRequest(targetVersion)
        )

        return currentVersion.data?.triggered
    }
}
