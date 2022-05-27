package de.solidblocks.rds.controller.model

import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.PROVIDERS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record5
import java.util.*

class ProvidersRepository(dsl: DSLContext) : BaseRepository(dsl) {

    val providers = PROVIDERS.`as`("providers")

    fun create(
        name: String, configValues: List<Pair<String, String>> = emptyList()
    ): RdsInstanceEntity? {
        val id = UUID.randomUUID()

        dsl.insertInto(PROVIDERS).columns(
            PROVIDERS.ID, PROVIDERS.NAME, PROVIDERS.DELETED
        ).values(id, name, false).execute()

        configValues.forEach {
            setConfiguration(ProviderId(id), it.first, it.second)
        }

        return read(id)!!
    }


    fun list(filter: Condition? = null): List<RdsInstanceEntity> {

        var filterConditions = providers.DELETED.isFalse

        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.PROVIDER)

        return dsl.selectFrom(
            providers.leftJoin(latest).on(providers.ID.eq(latest.field(CONFIGURATION_VALUES.PROVIDER)))
        ).where(filterConditions).fetchGroups({ it.into(providers) }, { it.into(latest) }).map {
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
        return list(providers.ID.eq(id)).firstOrNull()
    }

    fun read(name: String): RdsInstanceEntity? {
        return list(providers.NAME.eq(name)).firstOrNull()
    }

    fun exists(name: String): Boolean {
        return dsl.selectFrom(PROVIDERS).where(PROVIDERS.NAME.eq(name)).count() == 1
    }

    fun update(
        name: String, key: String, value: String?
    ): Boolean {
        val instance = read(name) ?: return false
        return setConfiguration(ProviderId(instance.id), key, value)
    }

    private fun update(
        id: UUID, key: String, value: String
    ) = setConfiguration(ProviderId(id), key, value)

}