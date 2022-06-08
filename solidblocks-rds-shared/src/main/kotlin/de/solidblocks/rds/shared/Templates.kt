package de.solidblocks.rds.shared

import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_31
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import mu.KotlinLogging
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

class Templates {

    companion object {

        private val logger = KotlinLogging.logger {}

        fun writeTemplate(
            templateFile: String,
            output: Path,
            variables: Map<String, String> = emptyMap(),
            basePackagePath: String = "/templates"
        ) {
            val cfg = Configuration(VERSION_2_3_31)

            cfg.setClassForTemplateLoading(Templates::class.java, basePackagePath)

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
    }
}