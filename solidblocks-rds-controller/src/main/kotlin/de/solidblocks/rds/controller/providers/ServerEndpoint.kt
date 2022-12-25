package de.solidblocks.rds.controller.providers

data class ServerEndpoint(val ipAddress: String, val agentPort: Int) {
    val agentAddress: String
        get() = "https://$ipAddress:$agentPort"
}
