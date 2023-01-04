package de.solidblocks.rds.controller.model.status

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.tables.references.STATUS
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.util.*

class StatusRepository(dsl: DSLContext) : BaseRepository(dsl) {

    fun update(
        id: UUID,
        health: HealthStatus
    ) = dsl.transactionResult { _ ->

        dsl.insertInto(STATUS).columns(
            STATUS.STATUS_ID,
            STATUS.ENTITY_ID,
            STATUS.STATUS_TIMESTAMP,
            STATUS.STATUS_HEALTH,
            STATUS.STATUS_PROVISIONING
        ).values(
            UUID.randomUUID(),
            id,
            LocalDateTime.now(),
            health.toString(),
            latest(id)?.statusProvisioning ?: ProvisioningStatus.UNKNOWN.toString()
        ).execute()
    }

    fun update(
        id: UUID,
        provisioning: ProvisioningStatus
    ) = dsl.transactionResult { _ ->


        dsl.insertInto(STATUS).columns(
            STATUS.STATUS_ID,
            STATUS.ENTITY_ID,
            STATUS.STATUS_TIMESTAMP,
            STATUS.STATUS_HEALTH,
            STATUS.STATUS_PROVISIONING
        ).values(
            UUID.randomUUID(),
            id,
            LocalDateTime.now(),
            latest(id)?.statusHealth ?: HealthStatus.UNKNOWN.toString(),
            provisioning.toString()
        ).execute()
    }

    fun latest(id: UUID) =
        dsl.selectFrom(STATUS).where(STATUS.ENTITY_ID.eq(id)).orderBy(STATUS.STATUS_TIMESTAMP.desc()).limit(1)
            .fetchOne()
}
