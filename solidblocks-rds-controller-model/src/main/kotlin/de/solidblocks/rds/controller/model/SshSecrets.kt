package de.solidblocks.rds.controller.model

data class SslSecrets(
    val controllerPrivateKey: String,
    val controllerPublicKey: String,
    val agentPrivateKey: String,
    val agentPublicKey: String,
)
