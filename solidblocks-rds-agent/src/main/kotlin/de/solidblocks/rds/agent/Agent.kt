package de.solidblocks.rds.agent

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

class PostgresInstance : CliktCommand() {

    val dataDir: Path by argument().path(canBeFile = false, mustExist = true)

    val backupDir: Path by argument().path(canBeFile = false, mustExist = true)

    override fun run() {
    }
}

fun main(args: Array<String>) = PostgresInstance().main(args)
