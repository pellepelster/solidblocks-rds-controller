package de.solidblocks.rds.controller.model

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.util.*

class ManagementTestDatabaseExtension : ParameterResolver, BeforeAllCallback {

    private val TEST_DB_JDBC_URL get() = "jdbc:derby:memory:${UUID.randomUUID()};create=true"

    private val database: ManagementDatabase = ManagementDatabase(TEST_DB_JDBC_URL)

    override fun beforeAll(context: ExtensionContext) {
        database.ensureDBSchema()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type
            .equals(ManagementDatabase::class.java)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return database
    }
}
