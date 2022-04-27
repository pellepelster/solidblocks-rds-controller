package de.solidblocks.rds.controller.model

import de.solidblocks.rds.controller.model.Constants.CONTROLLER_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.CONTROLLER_PUBLIC_KEY
import de.solidblocks.rds.controller.model.Constants.AGENT_PRIVATE_KEY
import de.solidblocks.rds.controller.model.Constants.AGENT_PUBLIC_KEY
import de.solidblocks.rds.controller.model.tables.records.ConfigurationValuesRecord
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.RDS_INSTANCES
import org.jooq.*
import org.jooq.impl.DSL
import java.util.*

class RdsInstancesRepository(val dsl: DSLContext) {

    val rdsInstances = RDS_INSTANCES.`as`("rds_instances")

    fun create(
        name: String, configValues: List<Pair<String, String>> = emptyList()
    ): RdsInstanceEntity? {
        val id = UUID.randomUUID()

        dsl.insertInto(RDS_INSTANCES).columns(
            RDS_INSTANCES.ID, RDS_INSTANCES.NAME, RDS_INSTANCES.DELETED
        ).values(id, name, false).execute()

        configValues.forEach {
            setConfigurationValue(id, it.first, it.second)
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
        return setConfigurationValue(instance.id, key, value)
    }

    private fun update(
        id: UUID, key: String, value: String
    ) = setConfigurationValue(id, key, value)

    /*
    private fun loadSslCredentials(list: List<Record5<UUID?, UUID?, String?, String?, Int?>>): SslSecrets {
        return SslSecrets(
            controllerPrivateKey = list.configValue(CONTROLLER_PRIVATE_KEY).value,
            controllerPublicKey = list.configValue(CONTROLLER_PUBLIC_KEY).value,
            agentPrivateKey = list.configValue(AGENT_PRIVATE_KEY).value,
            agentPublicKey = list.configValue(AGENT_PUBLIC_KEY).value,
        )
    }
    */

    private fun latestConfigurationValuesQuery(referenceColumn: TableField<ConfigurationValuesRecord, UUID?>): Table<Record5<UUID?, UUID?, String?, String?, Int?>> {

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

    private fun List<Record5<UUID?, UUID?, String?, String?, Int?>>.configValue(key: String): CloudConfigValue {
        return this.firstOrNull { it.getValue(CONFIGURATION_VALUES.KEY_) == key }?.map {
            CloudConfigValue(
                it.getValue(CONFIGURATION_VALUES.KEY_)!!,
                it.getValue(CONFIGURATION_VALUES.VALUE_)!!,
                it.getValue(CONFIGURATION_VALUES.VERSION)!!
            )
        }!!
    }

    private fun setConfigurationValue(id: UUID, key: String, value: String?): Boolean {


        // unfortunately derby does not support limits .limit(1).offset(0)
        val current = dsl.selectFrom(CONFIGURATION_VALUES)
            .where(CONFIGURATION_VALUES.KEY_.eq(key).and(CONFIGURATION_VALUES.RDS_INSTANCE.eq(id)))
            .orderBy(CONFIGURATION_VALUES.VERSION.desc()).fetch()

        val record = dsl.insertInto(CONFIGURATION_VALUES).columns(
            CONFIGURATION_VALUES.ID,
            CONFIGURATION_VALUES.VERSION,
            CONFIGURATION_VALUES.RDS_INSTANCE,
            CONFIGURATION_VALUES.KEY_,
            CONFIGURATION_VALUES.VALUE_
        ).values(UUID.randomUUID(), current.firstOrNull()?.let { it.version!! + 1 } ?: 0, id, key, value).execute()

        return record == 1
    }
}