package de.solidblocks.rds.controller.model.repositories

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.tables.references.LOG
import org.jooq.DSLContext
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.util.*

class LogRepository(dsl: DSLContext) : BaseRepository(dsl) {

    fun log(
        id: UUID,
        level: Level,
        message: String
    ) = dsl.transactionResult { _ ->
        dsl.insertInto(LOG).columns(
            LOG.LOG_ID,
            LOG.ENTITY_ID,
            LOG.LOG_TIME,
            LOG.LOG_LEVEL,
            LOG.LOG_MESSAGE
        ).values(
            UUID.randomUUID(),
            id,
            LocalDateTime.now(),
            level.toString(),
            message,
        ).execute()
    }

}
