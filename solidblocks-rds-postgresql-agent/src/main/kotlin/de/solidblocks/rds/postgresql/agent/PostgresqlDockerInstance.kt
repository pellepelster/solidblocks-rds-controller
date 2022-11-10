package de.solidblocks.rds.postgresql.agent

import de.solidblocks.rds.docker.DockerManager
import de.solidblocks.rds.docker.HealthChecks
import java.nio.file.Path
import java.util.*

class PostgresqlDockerInstance(
    val id: UUID,
    databaseName: String,
    dataDir: Path,
    backupDir: Path,
    username: String,
    password: String
) {

    private val dockerManager = DockerManager(
        id = databaseName,
        ports = setOf(5432),
        dockerImage = "pellepelster/solidblocks-rds-postgresql:v0.0.65",
        environment = mapOf(
            "DB_INSTANCE_NAME" to id.toString(),
            "DB_BACKUP_LOCAL" to "1",
            "DB_DATABASE_$databaseName" to databaseName,
            "DB_USERNAME_$databaseName" to username,
            "DB_PASSWORD_$databaseName" to password,
        ),
        bindings = mapOf(
            dataDir to "/storage/data",
            backupDir to "/storage/backup"
        ),
        healthCheck = HealthChecks.checkPostgres(databaseName, username, password)
    )

    fun start(): Boolean {
        return dockerManager.start()
    }

    fun isHealthy(): Boolean {
        return dockerManager.isHealthy()
    }

    fun isRunning(): Boolean {
        return dockerManager.isRunning()
    }

    fun stop() = dockerManager.stop()
}
