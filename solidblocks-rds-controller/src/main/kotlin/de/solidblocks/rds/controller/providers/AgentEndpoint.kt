package de.solidblocks.rds.controller.providers

data class AgentEndpoint(
    val endpoint: ServerEndpoint,
    val caServerPublicKey: String,
    val caClientPrivateKey: String,
    val caClientPublicKey: String
) {
    val agentAddress: String
        get() = endpoint.agentAddress
}
