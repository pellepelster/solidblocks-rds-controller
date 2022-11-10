package de.solidblocks.rds.postgresql.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path

class PostgresqlAgentCommand : CliktCommand(name = "postgres") {

    private val logger = KotlinLogging.logger {}

    val dataDir: Path by argument().path(canBeFile = false, mustExist = true).default(
        Path.of(System.getenv("SOLIDBLOCKS_DATA_DIR") ?: "/storage/data")
    )

    val backupDir: Path by argument().path(canBeFile = false, mustExist = true).default(
        Path.of(System.getenv("SOLIDBLOCKS_BACKUP_DIR") ?: "/storage/backup")
    )

    val solidblocksDir: Path by argument().path(canBeFile = false, mustExist = true).default(
        Path.of(System.getenv("SOLIDBLOCKS_DIR") ?: "/solidblocks")
    )

    val solidblocksProtectedDirectory: String
        get() = "$solidblocksDir/protected"

    val caPublicKey: String
        get() = "$solidblocksProtectedDirectory/solidblocks_client_ca_public_key.crt"

    val serverPrivateKey: String
        get() = "$solidblocksProtectedDirectory/solidblocks_server_private_key.key"

    val serverPublicKey: String
        get() = "$solidblocksProtectedDirectory/solidblocks_server_public_key.crt"

    override fun run() {

        if (!File(caPublicKey).exists()) {
            logger.error { "solidblocks file '$caPublicKey' not found" }
            throw ProgramResult(2)
        }

        if (!File(serverPrivateKey).exists()) {
            logger.error { "solidblocks file '$serverPrivateKey' not found" }
            throw ProgramResult(2)
        }

        if (!File(serverPublicKey).exists()) {
            logger.error { "solidblocks file '$serverPublicKey' not found" }
            throw ProgramResult(2)
        }

        PostgresqlAgent(
            8080,
            File(caPublicKey).readText(),
            File(serverPrivateKey).readText(),
            File(serverPublicKey).readText(),
            dataDir, backupDir
        ).waitForShutdownAndExit()
    }
}

fun main(args: Array<String>) = PostgresqlAgentCommand().main(args)
