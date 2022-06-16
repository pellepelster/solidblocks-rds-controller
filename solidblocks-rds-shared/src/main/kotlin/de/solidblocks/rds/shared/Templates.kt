package de.solidblocks.rds.shared

import freemarker.template.Configuration
import freemarker.template.Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX
import freemarker.template.Configuration.VERSION_2_3_31
import freemarker.template.TemplateExceptionHandler
import freemarker.template.Version
import mu.KotlinLogging
import java.io.FileWriter
import java.io.StringWriter
import java.io.Writer
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
            logger.info { "writing '$templateFile' to '$output" }

            FileWriter(output.toFile()).write(writeTemplate(templateFile, variables, basePackagePath))

            // TODO: do not set world readable when not run in test
            // TODO: also check x bits for all calls
            Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("r-xr-xr-x"))
        }

        fun writeTemplate(
            templateFile: String,
            variables: Map<String, String> = emptyMap(),
            basePackagePath: String = "/templates"
        ): String {
            val cfg = Configuration(VERSION_2_3_31)

            cfg.setClassForTemplateLoading(Templates::class.java, basePackagePath)

            cfg.incompatibleImprovements = Version(2, 3, 20)
            cfg.defaultEncoding = "UTF-8"
            cfg.locale = Locale.US
            cfg.templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
            cfg.interpolationSyntax = SQUARE_BRACKET_INTERPOLATION_SYNTAX

            val template = cfg.getTemplate(templateFile)

            // val consoleWriter: Writer = OutputStreamWriter(System.out)
            // template.process(variables, consoleWriter)

            return StringWriter().use {
                template.process(variables, it)
                it
            }.toString()
        }
    }
}
