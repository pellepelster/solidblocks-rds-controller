package de.solidblocks.rds.postgres

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

fun initWorldReadableTempDir(basename: String): File {
    val tempDir = "/tmp/$basename-temp-${UUID.randomUUID()}"

    File(tempDir).mkdirs()
    Files.setPosixFilePermissions(File(tempDir).toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

    return File(tempDir)
}
