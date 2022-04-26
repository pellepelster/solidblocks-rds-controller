package de.solidblocks.rds.controller

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.rds.controller.model.ManagementDatabase
import mu.KotlinLogging

class ControllerCommand : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    val managementDatabaseUrl: String
        get() {
            val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
            val dbPassword = config[ApplicationCli.DB_PASSWORD_KEY]
            val dbPath = config[ApplicationCli.DB_PATH_KEY]

            return "jdbc:derby:directory:$dbPath/mgmt;create=true;dataEncryption=true;encryptionKeyLength=256;encryptionAlgorithm=AES/CBC/NoPadding;bootPassword=$dbPassword;user=controller"
        }


    override fun run() {

        val db = ManagementDatabase(managementDatabaseUrl)
        db.ensureDBSchema()

        logger.info { "controller started" }
    }

}

