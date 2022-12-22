package de.solidblocks.rds.postgresql.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.rds.shared.dto.PostgresqlInstanceStatusResponse
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

data class RdsInstance(val id: UUID)
data class RdsInstanceConfig(val databaseName: String, val username: String, val password: String)

class RdsInstancesManager(
    val dataDir: Path,
    val backupDir: Path,
    val defaultDirectoryPermissions: String = "rwx------"
) {

    private val jackson = jacksonObjectMapper()

    private val logger = KotlinLogging.logger {}

    private val instances = ArrayList<PostgresqlDockerInstance>()

    fun list(): List<RdsInstance> {
        return dataDir.listDirectoryEntries().filter { it.isDirectory() }.mapNotNull {
            try {
                RdsInstance(UUID.fromString(it.name))
            } catch (e: Exception) {
                null
            }
        }
    }

    fun exists(id: UUID): Boolean {
        return get(id) != null
    }

    fun get(id: UUID): RdsInstanceConfig? {
        val instanceDir = dataDir.resolve(id.toString()).toFile()
        val instanceConfig = instanceDir.resolve("config.json")

        if (!instanceConfig.exists()) {
            return null
        }

        return try {
            jackson.readValue(instanceConfig, RdsInstanceConfig::class.java)
        } catch (e: Exception) {
            logger.error(e) { "failed to read rds-instance config '$instanceConfig'" }
            null
        }
    }

    fun create(id: UUID, name: String, username: String, password: String): Boolean {

        if (exists(id)) {
            return false
        }

        val dataDir = dataDir.resolve(id.toString())
        createDirectoryAndSetPermissions(dataDir)
        val backupDir = backupDir.resolve(id.toString())

        createDirectoryAndSetPermissions(backupDir)

        val instanceConfig = dataDir.resolve("config.json")
        try {
            jackson.writeValue(instanceConfig.toFile(), RdsInstanceConfig(name, username, password))
        } catch (e: Exception) {
            logger.error(e) { "failed to create rds-instance config '$instanceConfig'" }
            return false
        }

        return true
    }

    fun status(id: UUID): PostgresqlInstanceStatusResponse? {
        val instance = instances.firstOrNull { it.id == id } ?: return null
        return PostgresqlInstanceStatusResponse(id, instance.isRunning(), instance.isHealthy())
    }

    fun ensureInstances() {
        for (instance in list()) {

            if (!instances.any { instance.id == it.id }) {
                logger.info { "creating docker instance for rds-instance '${instance.id}'" }

                val config = get(instance.id) ?: continue

                val instanceDataDir = dataDir.resolve(instance.id.toString()).toFile()
                val instanceBackupDir = backupDir.resolve(instance.id.toString()).toFile()

                val dockerInstance = PostgresqlDockerInstance(
                    instance.id,
                    config.databaseName,
                    instanceDataDir.toPath(),
                    instanceBackupDir.toPath(),
                    config.username,
                    config.password
                )
                instances.add(dockerInstance)

                dockerInstance.start()
            }
        }
    }

    private fun createDirectoryAndSetPermissions(directory: Path): Boolean {

        if (!directory.exists()) {
            if (!directory.toFile().mkdirs()) {
                logger.error { "failed to create directory '$directory'" }
                return false
            }
        }

        try {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString(defaultDirectoryPermissions))
        } catch (e: Exception) {
            logger.error(e) { "failed to set permissions '$defaultDirectoryPermissions' for directory '$directory'" }
            return false
        }

        return true
    }
}
