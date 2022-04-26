package de.solidblocks.rds.postgres

import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission.*
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.path.createTempDirectory

class PostgresInstance(
    id: UUID,
    private val database: String,
    private val dataDir: Path,
    private val backupDir: Path,
    private val username: String = "user1",
    private val password: String = "password1"
) {

    val configTemplatesDir = createTempDirectory(
        id.toString(),
        PosixFilePermissions.asFileAttribute(
            EnumSet.of(OTHERS_READ, OTHERS_EXECUTE, OWNER_WRITE, OWNER_READ, OWNER_EXECUTE)
        )
    )

    private val dockerManager = DockerManager(
        id = database,
        ports = setOf(5432),
        dockerImage = "solidblocks-rds-postgres",
        environment = mapOf(
            "DB_DATABASE" to database,
            "DB_PASSWORD" to password,
            "DB_USERNAME" to username
        ),
        bindings = mapOf(
            configTemplatesDir to "/rds/config/templates",
            dataDir to "/storage/data",
            backupDir to "/storage/backup"
        ),
        healthCheck = HealthChecks.checkPostgres(database, username, password)
    )

    fun start(): Boolean {
        ConfigTemplates().pgHbaConf(configTemplatesDir, username)
        ConfigTemplates().pgbackrestConf(configTemplatesDir, database, dataDir, backupDir)
        ConfigTemplates().postgresqlConf(configTemplatesDir)

        return dockerManager.start()
    }

    fun stop() = dockerManager.stop()
}