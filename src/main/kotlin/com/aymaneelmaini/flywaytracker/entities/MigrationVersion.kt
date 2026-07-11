package com.aymaneelmaini.flywaytracker.entities

data class MigrationVersion(val parts: List<Int>) : Comparable<MigrationVersion> {

    companion object {
        fun parse(raw: String): MigrationVersion? {
            if (raw.isBlank()) return null
            val parsed = raw.replace('_', '.').split('.').map { it.toIntOrNull() ?: return null }
            return MigrationVersion(parsed)
        }
    }

    fun next(): MigrationVersion =
        MigrationVersion(parts.toMutableList().also { it[it.lastIndex] = it.last() + 1 })

    override fun compareTo(other: MigrationVersion): Int {
        val len = maxOf(parts.size, other.parts.size)
        for (i in 0 until len) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    // Equality ignores trailing zeros: V1.0 == V1
    private fun normalized() = parts.dropLastWhile { it == 0 }.ifEmpty { listOf(0) }
    override fun equals(other: Any?) = other is MigrationVersion && normalized() == other.normalized()
    override fun hashCode() = normalized().hashCode()
}
