package com.aymaneelmaini.flywaytracker.entity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class FlywayMigrationFileSpec : StringSpec({

    "parses a standard versioned migration" {
        val f = FlywayMigrationFile.from("V3__add_users_table.sql").shouldNotBeNull()
        f.prefix shouldBe 'V'
        f.version shouldBe MigrationVersion.parse("3")
        f.description shouldBe "add_users_table"
        f.extension shouldBe "sql"
        f.rawFileName shouldBe "V3__add_users_table.sql"
    }

    "parses a multi-part version" {
        val f = FlywayMigrationFile.from("V1.2.3__init.sql").shouldNotBeNull()
        f.version shouldBe MigrationVersion.parse("1.2.3")
    }

    "parses an undo migration (U prefix)" {
        val f = FlywayMigrationFile.from("U5__undo_users.sql").shouldNotBeNull()
        f.prefix shouldBe 'U'
        f.version shouldBe MigrationVersion.parse("5")
    }

    "strips the directory path and parses only the filename" {
        val f = FlywayMigrationFile.from("db/migration/V2__create_schema.sql").shouldNotBeNull()
        f.rawFileName shouldBe "V2__create_schema.sql"
        f.version shouldBe MigrationVersion.parse("2")
    }

    "accepts non-sql extensions" {
        FlywayMigrationFile.from("V1__init.java").shouldNotBeNull()
    }

    "returns null for an unknown prefix" {
        FlywayMigrationFile.from("X1__something.sql").shouldBeNull()
    }

    "returns null when the __ separator is missing" {
        FlywayMigrationFile.from("V3_add_users.sql").shouldBeNull()
    }

    "returns null when the version is not numeric" {
        FlywayMigrationFile.from("Vabc__init.sql").shouldBeNull()
    }

    "returns null when the description is blank" {
        FlywayMigrationFile.from("V1__.sql").shouldBeNull()
    }

    "returns null when the extension is missing" {
        FlywayMigrationFile.from("V1__init").shouldBeNull()
    }

    "returns null for blank input" {
        FlywayMigrationFile.from("").shouldBeNull()
    }

    // conflictsWith
    "detects a conflict when prefix and version both match" {
        val a = FlywayMigrationFile.from("V3__migration_a.sql")!!
        val b = FlywayMigrationFile.from("V3__migration_b.sql")!!
        a.conflictsWith(b) shouldBe true
    }

    "no conflict when versions differ" {
        val a = FlywayMigrationFile.from("V3__init.sql")!!
        val b = FlywayMigrationFile.from("V4__init.sql")!!
        a.conflictsWith(b) shouldBe false
    }

    "no conflict when prefixes differ" {
        val v = FlywayMigrationFile.from("V3__init.sql")!!
        val u = FlywayMigrationFile.from("U3__undo.sql")!!
        v.conflictsWith(u) shouldBe false
    }

    "detects conflict when versions differ only by trailing zeros (V1 vs V1.0)" {
        val a = FlywayMigrationFile.from("V1__a.sql")!!
        val b = FlywayMigrationFile.from("V1.0__b.sql")!!
        a.conflictsWith(b) shouldBe true
    }
})
