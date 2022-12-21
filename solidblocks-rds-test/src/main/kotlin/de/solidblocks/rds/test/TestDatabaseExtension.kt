package de.solidblocks.rds.test

import de.solidblocks.rds.base.Database
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.*

class TestDatabaseExtension : ParameterResolver, BeforeAllCallback {

    private val testJdbcUrl get() = "jdbc:derby:memory:${UUID.randomUUID()};create=true"

    private val database: Database = Database(testJdbcUrl)

    override fun beforeAll(context: ExtensionContext) {
        database.ensureDBSchema()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(Database::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return database
    }
}
