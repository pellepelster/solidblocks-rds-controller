package de.solidblocks.rds.controller.model

import de.solidblocks.rds.controller.model.tables.records.ConfigurationValuesRecord
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import org.jooq.DSLContext
import org.jooq.Record5
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.DSL
import java.util.*

abstract class BaseRepository(val dsl: DSLContext) {

    sealed class IdType(private val id: UUID)

    protected class ProviderId(val id: UUID) : IdType(id)

    protected class RdsInstanceId(val id: UUID) : IdType(id)

    protected class ControllerInstanceId(val id: UUID) : IdType(id)

    protected fun latestConfigurationValuesQuery(referenceColumn: TableField<ConfigurationValuesRecord, UUID?>): Table<Record5<UUID?, UUID?, String?, String?, Int?>> {

        val latestVersions = dsl.select(
            referenceColumn,
            CONFIGURATION_VALUES.KEY_,
            DSL.max(CONFIGURATION_VALUES.VERSION).`as`(CONFIGURATION_VALUES.VERSION)
        ).from(CONFIGURATION_VALUES).groupBy(referenceColumn, CONFIGURATION_VALUES.KEY_).asTable("latest_versions")

        val latest = dsl.select(
            referenceColumn,
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.KEY_,
            CONFIGURATION_VALUES.VALUE_,
            CONFIGURATION_VALUES.VERSION
        ).from(
            CONFIGURATION_VALUES.rightJoin(latestVersions).on(
                CONFIGURATION_VALUES.KEY_.eq(latestVersions.field(CONFIGURATION_VALUES.KEY_)).and(
                    CONFIGURATION_VALUES.VERSION.eq(latestVersions.field(CONFIGURATION_VALUES.VERSION))
                        .and(referenceColumn.eq(latestVersions.field(referenceColumn)))
                )
            )
        ).where(referenceColumn.isNotNull).asTable("latest_configurations")

        return latest
    }

    protected fun setConfiguration(id: IdType, key: String, value: String?): Boolean {

        var providerId: UUID? = null
        var rdsInstanceId: UUID? = null
        var controllerId: UUID? = null

        when (id) {
            is ProviderId -> providerId = id.id
            is RdsInstanceId -> rdsInstanceId = id.id
            is ControllerInstanceId -> controllerId = id.id
        }

        val condition = when (id) {
            is ProviderId -> CONFIGURATION_VALUES.PROVIDER.eq(id.id)
            is RdsInstanceId -> CONFIGURATION_VALUES.RDS_INSTANCE.eq(id.id)
            is ControllerInstanceId -> CONFIGURATION_VALUES.CONTROLLER.eq(id.id)
        }

        // unfortunately derby does not support limits .limit(1).offset(0)
        val current = dsl.selectFrom(CONFIGURATION_VALUES)
            .where(CONFIGURATION_VALUES.KEY_.eq(key).and(condition))
            .orderBy(CONFIGURATION_VALUES.VERSION.desc()).fetch()

        val result = dsl.insertInto(CONFIGURATION_VALUES).columns(
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.VERSION,
            CONFIGURATION_VALUES.PROVIDER,
            CONFIGURATION_VALUES.RDS_INSTANCE,
            CONFIGURATION_VALUES.CONTROLLER,
            CONFIGURATION_VALUES.KEY_,
            CONFIGURATION_VALUES.VALUE_
        ).values(
            UUID.randomUUID(),
            current.firstOrNull()?.let { it.version!! + 1 }
                ?: 0,
            providerId, rdsInstanceId, controllerId, key, value
        ).execute()

        return result == 1
    }
}
