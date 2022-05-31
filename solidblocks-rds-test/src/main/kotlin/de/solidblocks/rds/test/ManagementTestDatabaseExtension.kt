package de.solidblocks.rds.test

import de.solidblocks.rds.base.Database
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.*

class ManagementTestDatabaseExtension : ParameterResolver, BeforeAllCallback {

    private val TEST_DB_JDBC_URL get() = "jdbc:derby:memory:${UUID.randomUUID()};create=true"

    private val database: Database = Database(TEST_DB_JDBC_URL)

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
