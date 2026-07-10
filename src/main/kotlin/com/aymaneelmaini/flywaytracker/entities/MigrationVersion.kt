package com.aymaneelmaini.flywaytracker.entities

data class MigrationVersion(val parts: List<Int>) {

    companion object {
        fun parse(raw: String): MigrationVersion? {
            if (raw.isBlank()) return null
            val normalized = raw.replace('_', '.')
            val parsed = normalized.split('.').map { it.toIntOrNull() ?: return null }
            if (parsed.isEmpty()) return null
            return MigrationVersion(parsed)
        }
    }

    fun next(): MigrationVersion {
        val newParts = parts.toMutableList()
        newParts[newParts.lastIndex] = newParts.last() + 1
        return MigrationVersion(newParts)
    }

    override fun toString(): String = parts.joinToString(".")

    override fun equals(other: Any?): Boolean {
        if (other !is MigrationVersion) return false
        val a = parts.dropLastWhile { it == 0 }.ifEmpty { listOf(0) }
        val b = other.parts.dropLastWhile { it == 0 }.ifEmpty { listOf(0) }
        return a == b
    }

    override fun hashCode(): Int =
        parts.dropLastWhile { it == 0 }.ifEmpty { listOf(0) }.hashCode()
}
