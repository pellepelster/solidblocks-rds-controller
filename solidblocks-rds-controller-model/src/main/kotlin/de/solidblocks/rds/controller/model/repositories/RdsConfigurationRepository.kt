package de.solidblocks.rds.controller.model.repositories

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.entities.RdsConfigurationEntity
import de.solidblocks.rds.controller.model.entities.RdsConfigurationId
import de.solidblocks.rds.controller.model.entities.RdsInstanceId
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.RDS_CONFIGURATIONS
import de.solidblocks.rds.controller.model.tables.references.RDS_INSTANCES
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*

class RdsConfigurationRepository(dsl: DSLContext) : BaseRepository(dsl) {

    private val rdsInstances = RDS_CONFIGURATIONS.`as`("rds_configurations")

    fun create(
        rdsInstanceId: RdsInstanceId,
        configValues: Map<String, String> = emptyMap()
    ) = dsl.transactionResult { _ ->
        val id = UUID.randomUUID()

        dsl.insertInto(rdsInstances).columns(
            rdsInstances.ID,
            rdsInstances.RDS_INSTANCE,
        ).values(id, rdsInstanceId.id).execute()

        configValues.forEach {
            setConfiguration(RdsConfigurationId(id), it.key, it.value)
        }

        read(id) ?: run { throw RuntimeException("could not read created rds configuration") }
    }

    fun list(filter: Condition? = null): List<RdsConfigurationEntity> {

        var filterConditions = rdsInstances.DELETED.isFalse
        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.RDS_INSTANCE)

        return dsl.selectFrom(
            rdsInstances.leftJoin(latest).on(rdsInstances.ID.eq(latest.field(CONFIGURATION_VALUES.RDS_INSTANCE)))
        ).where(filterConditions).fetchGroups({ it.into(rdsInstances) }, { it.into(latest) }).map {
            RdsConfigurationEntity(
                id = RdsConfigurationId(it.key.id!!),
                rdsInstance = RdsInstanceId(it.key.rdsInstance!!),
                configValues = it.value.mapNotNull {
                    if (it.getValue(CONFIGURATION_VALUES.KEY_) == null) {
                        null
                    } else {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.KEY_)!!,
                            it.getValue(CONFIGURATION_VALUES.VALUE_),
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    }
                },
            )
        }
    }

    fun delete(id: UUID): Boolean {
        dsl.delete(CONFIGURATION_VALUES).where(CONFIGURATION_VALUES.RDS_INSTANCE.eq(id)).execute()
        return dsl.delete(RDS_INSTANCES).where(RDS_INSTANCES.ID.eq(id)).execute() == 1
    }

    fun exists(name: String): Boolean {
        return dsl.selectFrom(RDS_INSTANCES).where(RDS_INSTANCES.NAME.eq(name)).count() == 1
    }

    fun update(id: UUID, values: Map<String, String>): Boolean {
        return values.map {
            update(id, it.key, it.value)
        }.all { it }
    }

    fun read(id: UUID): RdsConfigurationEntity? {
        return list(rdsInstances.ID.eq(id)).firstOrNull()
    }

    fun list(id: RdsInstanceId) = list(rdsInstances.RDS_INSTANCE.eq(id.id))

    private fun update(
        id: UUID,
        key: String,
        value: String
    ) = setConfiguration(RdsInstanceId(id), key, value)
}
