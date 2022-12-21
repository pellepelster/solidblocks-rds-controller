package de.solidblocks.rds.controller.model.controllers

import de.solidblocks.rds.controller.model.BaseRepository
import de.solidblocks.rds.controller.model.CloudConfigValue
import de.solidblocks.rds.controller.model.tables.references.CONFIGURATION_VALUES
import de.solidblocks.rds.controller.model.tables.references.CONTROLLERS
import org.jooq.Condition
import org.jooq.DSLContext
import java.util.*

class ControllersRepository(dsl: DSLContext) : BaseRepository(dsl) {

    private val controllers = CONTROLLERS.`as`("controllers")

    fun create(
        name: String,
        configValues: Map<String, String> = emptyMap()
    ) = dsl.transactionResult { _ ->
        val id = UUID.randomUUID()

        dsl.insertInto(controllers).columns(
            CONTROLLERS.ID, CONTROLLERS.NAME, CONTROLLERS.DELETED
        ).values(id, name, false).execute()

        configValues.forEach {
            setConfiguration(ControllerInstanceId(id), it.key, it.value)
        }

        read(id) ?: run { throw RuntimeException("could not read created controller") }
    }

    fun list(filter: Condition? = null): List<ControllerEntity> {

        var filterConditions = controllers.DELETED.isFalse
        if (filter != null) {
            filterConditions = filterConditions.and(filter)
        }

        val latest = latestConfigurationValuesQuery(CONFIGURATION_VALUES.CONTROLLER)

        return dsl.selectFrom(
            controllers.leftJoin(latest).on(controllers.ID.eq(latest.field(CONFIGURATION_VALUES.CONTROLLER)))
        ).where(filterConditions).fetchGroups({ it.into(controllers) }, { it.into(latest) }).map {
            ControllerEntity(
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

    fun exists(name: String): Boolean {
        return dsl.selectFrom(CONTROLLERS).where(CONTROLLERS.NAME.eq(name).and(CONTROLLERS.DELETED.isFalse))
            .count() == 1
    }

    fun delete(id: UUID) =
        dsl.update(CONTROLLERS).set(CONTROLLERS.DELETED, true).where(CONTROLLERS.ID.eq(id)).execute() == 1

    fun update(id: UUID, values: Map<String, String>): Boolean {
        return values.map {
            update(id, it.key, it.value)
        }.all { it }
    }

    fun read(id: UUID): ControllerEntity? {
        return list(controllers.ID.eq(id)).firstOrNull()
    }

    fun read(name: String): ControllerEntity? {
        return list(controllers.NAME.eq(name)).firstOrNull()
    }

    fun update(
        name: String,
        key: String,
        value: String?
    ): Boolean {
        val instance = read(name) ?: return false
        return setConfiguration(ControllerInstanceId(instance.id), key, value)
    }

    private fun update(
        id: UUID,
        key: String,
        value: String
    ) = setConfiguration(ControllerInstanceId(id), key, value)
}
