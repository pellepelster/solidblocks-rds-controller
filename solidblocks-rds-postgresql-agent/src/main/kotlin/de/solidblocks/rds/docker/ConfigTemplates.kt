package de.solidblocks.rds.docker

import de.solidblocks.rds.shared.Templates
import java.nio.file.Path

class ConfigTemplates {

    fun pgbackrestConf(configTemplatesDir: Path, database: String, dataDir: Path, backupDir: Path) {
        Templates.writeTemplate(
            "config/templates/pgbackrest.conf.ftl", configTemplatesDir.resolve("pgbackrest.conf"),
            mapOf("DB_DATABASE" to database, "DATA_DIR" to dataDir.toString(), "BACKUP_DIR" to backupDir.toString())
        )
    }

    fun pgHbaConf(configTemplatesDir: Path, username: String) {
        Templates.writeTemplate(
            "config/templates/pg_hba.conf.ftl", configTemplatesDir.resolve("pg_hba.conf"),
            mapOf("USERNAME" to username)
        )
    }

    fun postgresqlConf(configTemplatesDir: Path) {
        Templates.writeTemplate(
            "config/templates/postgresql.conf.ftl", configTemplatesDir.resolve("postgresql.conf"),
            mapOf()
        )
    }
}
