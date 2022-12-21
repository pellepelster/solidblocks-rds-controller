package de.solidblocks.rds.controller.model.status

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.tables.references.STATUS
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.util.*

class StatusRepository(dsl: DSLContext) : BaseRepository(dsl) {

    fun update(
        id: UUID,
        status: Status
    ) = dsl.transactionResult { _ ->
        dsl.insertInto(STATUS).columns(
            STATUS.ID,
            STATUS.ENTITY,
            STATUS.STATUS_TIME,
            STATUS.STATUS_,
        ).values(UUID.randomUUID(), id, LocalDateTime.now(), status.toString()).execute()
    }

    fun latest(id: UUID) =
        dsl.selectFrom(STATUS).where(STATUS.ENTITY.eq(id)).orderBy(STATUS.STATUS_TIME.desc()).limit(1).fetchOne()
}
