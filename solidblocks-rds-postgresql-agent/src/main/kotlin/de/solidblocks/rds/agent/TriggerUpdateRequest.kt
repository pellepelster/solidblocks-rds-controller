package de.solidblocks.rds.agent

data class TriggerUpdateRequest(val updateVersion: String) {
    companion object {
        const val TRIGGER_UPDATE_PATH = "/trigger-update"
    }
}
