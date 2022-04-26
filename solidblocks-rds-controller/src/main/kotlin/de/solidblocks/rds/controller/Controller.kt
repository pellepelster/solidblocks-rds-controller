package de.solidblocks.rds.controller

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import de.solidblocks.rds.controller.model.tables.Instances
import mu.KotlinLogging

class Controller : CliktCommand() {

    private val logger = KotlinLogging.logger {}

    val dbDir by option().path(canBeFile = false, mustExist = true)
        .help("fully qualified path where the mgmt database will be stored")
        .required()

    override fun run() {
        logger.info { "controller started" }
        Instances
    }
}

fun main(args: Array<String>) = Controller().main(args)
