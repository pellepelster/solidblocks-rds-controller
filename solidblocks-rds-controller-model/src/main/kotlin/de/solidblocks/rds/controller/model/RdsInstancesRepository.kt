package de.solidblocks.rds.controller.model

import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.RDS_INSTANCES
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record5
import java.util.*

class RdsInstancesRepository(dsl: DSLContext) : BaseRepository(dsl) {

    val rdsInstances = RDS_INSTANCES.`as`("rds_instances")

    fun create(
        name: String, configValues: List<Pair<String, String>> = emptyList()
    ): RdsInstanceEntity? {
        val id = UUID.randomUUID()

        dsl.insertInto(RDS_INSTANCES).columns(
            RDS_INSTANCES.ID, RDS_INSTANCES.NAME, RDS_INSTANCES.DELETED
        ).values(id, name, false).execute()

        configValues.forEach {
            setConfiguration(RdsInstanceId(id), it.first, it.second)
        }

        return read(id)
    }


    fun list(filter: Condition? = null): List<RdsInstanceEntity> {

        var filterConditions = rdsInstances.DELETED.isFalse
        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.RDS_INSTANCE)

        return dsl.selectFrom(
            rdsInstances.leftJoin(latest).on(rdsInstances.ID.eq(latest.field(CONFIGURATION_VALUES.RDS_INSTANCE)))
        ).where(filterConditions).fetchGroups({ it.into(rdsInstances) }, { it.into(latest) }).map {
            RdsInstanceEntity(
                id = it.key.id!!,
                name = it.key.name!!,
                configValues = it.value.map {
                    if (it.getValue(CONFIGURATION_VALUES.KEY_) == null) {
                        null
                    } else {
                        CloudConfigValue(
                            it.getValue(CONFIGURATION_VALUES.KEY_)!!,
                            it.getValue(CONFIGURATION_VALUES.VALUE_),
                            it.getValue(CONFIGURATION_VALUES.VERSION)!!
                        )
                    }
                }.filterNotNull(),
            )
        }
    }

    fun update(id: UUID, values: Map<String, String>): Boolean {
        return values.map {
            update(id, it.key, it.value)
        }.all { it }
    }

    fun read(id: UUID): RdsInstanceEntity? {
        return list(rdsInstances.ID.eq(id)).firstOrNull()
    }

    fun read(name: String): RdsInstanceEntity? {
        return list(rdsInstances.NAME.eq(name)).firstOrNull()
    }

    fun update(
        name: String, key: String, value: String?
    ): Boolean {
        val instance = read(name) ?: return false
        return setConfiguration(RdsInstanceId(instance.id), key, value)
    }

    private fun update(
        id: UUID, key: String, value: String
    ) = setConfiguration(RdsInstanceId(id), key, value)

}