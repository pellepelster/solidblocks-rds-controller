package de.solidblocks.rds.controller

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import mu.KotlinLogging

class ApplicationCli : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val DB_PASSWORD_KEY = "dbPassword"
        const val DB_PATH_KEY = "dbPath"
    }

    val dbPassword: String by option(help = "secret for the solidblocks rds mgmt db").required()

    val dbPath by option().path(canBeFile = false, mustExist = true)
        .help("fully qualified path where the mgmt database will be stored")
        .required()

    override fun run() {

        val config = currentContext.findOrSetObject { mutableMapOf<String, String>() }
        config[DB_PASSWORD_KEY] = dbPassword
        config[DB_PATH_KEY] = dbPath.toAbsolutePath().toString()
    }
}

fun main(args: Array<String>) = ApplicationCli().subcommands(ControllerCommand()).main(args)
