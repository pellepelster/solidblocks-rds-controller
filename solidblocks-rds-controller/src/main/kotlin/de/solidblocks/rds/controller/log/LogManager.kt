package de.solidblocks.rds.controller.log

import de.solidblocks.rds.controller.model.repositories.LogRepository
import mu.KLogger
import org.slf4j.event.Level
import java.util.*

class LogManager(val id: UUID, val repository: LogRepository, val logger: KLogger) {

    fun log(level: Level, message: String) {
        repository.log(id, level, message)
    }

    fun info(msg: () -> String) {
        with(msg.invoke()) {
            log(Level.INFO, this)
            logger.info { this }
        }
    }

    fun warn(msg: () -> String) {
        with(msg.invoke()) {
            log(Level.WARN, this)
            logger.warn { this }
        }
    }

    fun error(msg: () -> String) {
        with(msg.invoke()) {
            log(Level.ERROR, this)
            logger.error { this }
        }
    }

}