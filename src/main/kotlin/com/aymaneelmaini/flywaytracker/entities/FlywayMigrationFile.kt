package com.aymaneelmaini.flywaytracker.entities

private val VALID_PREFIXES = setOf('V', 'U', 'R')

data class FlywayMigrationFile(
    val prefix: Char,
    val version: MigrationVersion,
    val description: String,
    val extension: String,
    val rawFileName: String
) {
    companion object {
        fun from(path: String): FlywayMigrationFile? {
            val name = path.substringAfterLast("/").substringAfterLast("\\")
            val prefix = name.firstOrNull() ?: return null
            if (prefix !in VALID_PREFIXES) return null
            val sep = name.indexOf("__").takeIf { it >= 2 } ?: return null
            val version = MigrationVersion.parse(name.substring(1, sep)) ?: return null
            val rest = name.substring(sep + 2)
            val dot = rest.lastIndexOf('.')
            if (dot < 0) return null
            val description = rest.substring(0, dot)
            val extension = rest.substring(dot + 1)
            if (description.isBlank() || extension.isBlank()) return null
            return FlywayMigrationFile(prefix, version, description, extension, name)
        }
    }

    fun conflictsWith(other: FlywayMigrationFile): Boolean =
        prefix == other.prefix && version == other.version
}
