package de.solidblocks.rds.docker

import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import mu.KotlinLogging
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

class ConfigTemplates {

    private val logger = KotlinLogging.logger {}

    private fun writeTemplate(templateFile: String, output: Path, variables: Map<String, String>) {
        val cfg = Configuration()

        cfg.setClassForTemplateLoading(ConfigTemplates::class.java, "/templates")

        cfg.incompatibleImprovements = Version(2, 3, 20)
        cfg.defaultEncoding = "UTF-8"
        cfg.locale = Locale.US
        cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER

        val template = cfg.getTemplate(templateFile)

        // val consoleWriter: Writer = OutputStreamWriter(System.out)
        // template.process(variables, consoleWriter)

        logger.info { "writing '$templateFile' to '$output" }

        FileWriter(output.toFile()).use { fileWriter ->
            template.process(variables, fileWriter)
        }

        // TODO: do not set world readable when not run in test
        Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("r--r--r--"))
    }

    fun pgbackrestConf(configTemplatesDir: Path, database: String, dataDir: Path, backupDir: Path) {
        writeTemplate(
            "config/templates/pgbackrest.conf.ftl", configTemplatesDir.resolve("pgbackrest.conf"),
            mapOf("DB_DATABASE" to database, "DATA_DIR" to dataDir.toString(), "BACKUP_DIR" to backupDir.toString())
        )
    }

    fun pgHbaConf(configTemplatesDir: Path, username: String) {
        writeTemplate(
            "config/templates/pg_hba.conf.ftl", configTemplatesDir.resolve("pg_hba.conf"),
            mapOf("USERNAME" to username)
        )
    }

    fun postgresqlConf(configTemplatesDir: Path) {
        writeTemplate(
            "config/templates/postgresql.conf.ftl", configTemplatesDir.resolve("postgresql.conf"),
            mapOf()
        )
    }
}
