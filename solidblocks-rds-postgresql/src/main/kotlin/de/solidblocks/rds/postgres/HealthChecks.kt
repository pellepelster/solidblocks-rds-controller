package de.solidblocks.rds.postgres

import mu.KotlinLogging
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.sql.DriverManager

object HealthChecks {

    private val logger = KotlinLogging.logger {}

    fun checkPort(address: InetSocketAddress) = try {
        Socket().use { socket ->
            socket.connect(address, 1000)
            logger.info { "port ${address.hostName}:${address.port} is listening" }
            true
        }
    } catch (e: IOException) {
        logger.warn { "port ${address.hostName}:${address.port} is not listening" }
        false
    }

    fun checkPostgres(
        database: String,
        user: String,
        password: String
    ): (address: InetSocketAddress) -> Boolean = check@{ address ->
        try {
            val url =
                "jdbc:postgresql://${address.hostName}:${address.port}/$database?user=$user&password=$password"

            DriverManager.getConnection(url).use { connection ->

                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT 1").use { resultSet ->
                        logger.info { "postgres check for ${address.address}:${address.port} was ok" }
                        return@check true
                    }
                }
            }
        } catch (e: IOException) {
            logger.warn { "postgres check failed for ${address.address}:${address.port}: '${e.message}'" }
        }

        return@check false
    }
}
