package com.aymaneelmaini.flywaytracker.entities

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
            if (name.length < 5) return null
            val prefix = name[0]
            if (prefix !in listOf('V', 'U', 'R')) return null
            val separatorIndex = name.indexOf("__")
            if (separatorIndex < 2) return null
            val versionStr = name.substring(1, separatorIndex)
            val version = MigrationVersion.parse(versionStr) ?: return null
            val afterSeparator = name.substring(separatorIndex + 2)
            val lastDot = afterSeparator.lastIndexOf('.')
            if (lastDot < 0) return null
            val description = afterSeparator.substring(0, lastDot)
            val extension = afterSeparator.substring(lastDot + 1)
            if (description.isBlank() || extension.isBlank()) return null
            return FlywayMigrationFile(prefix, version, description, extension, name)
        }

        fun isValidName(name: String): Boolean = from(name) != null
    }

    fun conflictsWith(other: FlywayMigrationFile): Boolean =
        prefix == other.prefix && version == other.version
}
