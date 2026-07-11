package com.aymaneelmaini.flywaytracker.service

import com.aymaneelmaini.flywaytracker.entity.FlywayMigrationFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FlywayMigrationServicePureSpec : StringSpec({

    fun file(name: String): FlywayMigrationFile = FlywayMigrationFile.from(name)!!

    "nextVersionFrom returns V1__ when the list is empty" {
        nextVersionFrom(emptyList()) shouldBe "V1__"
    }

    "nextVersionFrom increments past a single versioned migration" {
        nextVersionFrom(listOf(file("V1__init.sql"))) shouldBe "V2__"
    }

    "nextVersionFrom picks the max V-prefix version from an unsorted list" {
        nextVersionFrom(listOf(
            file("V1__init.sql"),
            file("V3__add_roles.sql"),
            file("V2__add_users.sql"),
        )) shouldBe "V4__"
    }

    "nextVersionFrom ignores U-prefix (undo) migrations" {
        nextVersionFrom(listOf(
            file("V2__create_schema.sql"),
            file("U2__undo_create_schema.sql"),
        )) shouldBe "V3__"
    }

    "nextVersionFrom ignores R-prefix (repeatable) migrations" {
        nextVersionFrom(listOf(
            file("V5__add_index.sql"),
            file("R__refresh_view.sql"),
        )) shouldBe "V6__"
    }

    "nextVersionFrom returns V1__ when only non-V migrations are present" {
        nextVersionFrom(listOf(file("U3__undo.sql"))) shouldBe "V1__"
    }

    "nextVersionFrom handles multi-part versions correctly" {
        nextVersionFrom(listOf(file("V1.2.3__patch.sql"))) shouldBe "V1.2.4__"
    }

    "nextVersionFrom deduplicates V1 and V1.0 as the same version — next is V2" {
        nextVersionFrom(listOf(
            file("V1__a.sql"),
            file("V1.0__b.sql"),
        )) shouldBe "V2__"
    }
})

private fun nextVersionFrom(migrations: List<FlywayMigrationFile>): String {
    val max = migrations.filter { it.prefix == 'V' }.maxByOrNull { it.version }?.version
    return if (max != null) "V${max.next()}__" else "V1__"
}
