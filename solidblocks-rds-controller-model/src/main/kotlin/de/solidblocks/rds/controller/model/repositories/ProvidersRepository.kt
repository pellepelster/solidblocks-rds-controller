package de.solidblocks.rds.controller.model.repositories

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.entities.ControllerEntity
import de.solidblocks.rds.controller.model.entities.ControllerId
import de.solidblocks.rds.controller.model.entities.ProviderEntity
import de.solidblocks.rds.controller.model.entities.ProviderId
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.PROVIDERS
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*

class ProvidersRepository(dsl: DSLContext) : BaseRepository(dsl) {

    val providers = PROVIDERS.`as`("providers")

    fun create(
        name: String,
        controller: ControllerEntity,
        configValues: Map<String, String> = emptyMap()
    ) = dsl.transactionResult { _ ->
        val id = UUID.randomUUID()

        dsl.insertInto(PROVIDERS).columns(
            PROVIDERS.ID, PROVIDERS.CONTROLLER, PROVIDERS.NAME, PROVIDERS.DELETED
        ).values(id, controller.id.id, name, false).execute()

        configValues.forEach {
            setConfiguration(ProviderId(id), it.key, it.value)
        }

        read(id) ?: run { throw RuntimeException("could not read created provider") }
    }

    fun list(filter: Condition? = null): List<ProviderEntity> = listInternal(providers.DELETED.isFalse.and(filter))

    fun listDeleted(): List<ProviderEntity> = listInternal(providers.DELETED.isTrue)

    private fun listInternal(filterCondition: Condition): List<ProviderEntity> {

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.PROVIDER)

        return dsl.selectFrom(
            providers.leftJoin(latest).on(providers.ID.eq(latest.field(CONFIGURATION_VALUES.PROVIDER)))
        ).where(filterCondition).fetchGroups({ it.into(providers) }, { it.into(latest) }).map {
            ProviderEntity(
                id = ProviderId(it.key.id!!),
                name = it.key.name!!,
                controller = ControllerId(it.key.controller!!),
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

    fun update(id: UUID, values: Map<String, String>): Boolean {
        return values.map {
            update(id, it.key, it.value)
        }.all { it }
    }

    fun delete(id: UUID): Boolean {
        return dsl.update(PROVIDERS).set(PROVIDERS.DELETED, true).where(PROVIDERS.ID.eq(id)).execute() == 1
    }

    fun read(id: UUID): ProviderEntity? {
        return list(providers.ID.eq(id)).firstOrNull()
    }

    fun read(name: String): ProviderEntity? {
        return list(providers.NAME.eq(name)).firstOrNull()
    }

    fun exists(name: String): Boolean {
        return dsl.selectFrom(PROVIDERS).where(PROVIDERS.NAME.eq(name).and(PROVIDERS.DELETED.isFalse)).count() == 1
    }

    fun update(
        name: String,
        key: String,
        value: String?
    ): Boolean {
        val provider = read(name) ?: return false
        return setConfiguration(provider.id, key, value)
    }

    private fun update(
        id: UUID,
        key: String,
        value: String
    ) = setConfiguration(ProviderId(id), key, value)
}
