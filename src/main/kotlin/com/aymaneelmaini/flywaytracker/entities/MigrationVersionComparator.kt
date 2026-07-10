package com.aymaneelmaini.flywaytracker.entities

object MigrationVersionComparator : Comparator<MigrationVersion> {
    override fun compare(a: MigrationVersion, b: MigrationVersion): Int {
        val maxLen = maxOf(a.parts.size, b.parts.size)
        for (i in 0 until maxLen) {
            val aPart = a.parts.getOrElse(i) { 0 }
            val bPart = b.parts.getOrElse(i) { 0 }
            if (aPart != bPart) return aPart.compareTo(bPart)
        }
        return 0
    }
}
