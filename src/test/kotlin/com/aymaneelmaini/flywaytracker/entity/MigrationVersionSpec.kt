package com.aymaneelmaini.flywaytracker.entity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MigrationVersionSpec : StringSpec({

    "parses a single integer" {
        MigrationVersion.parse("3")?.parts shouldBe listOf(3)
    }

    "parses dot-separated parts" {
        MigrationVersion.parse("1.2.3")?.parts shouldBe listOf(1, 2, 3)
    }

    "parses underscore-separated parts (Flyway alternative)" {
        MigrationVersion.parse("1_2_3")?.parts shouldBe listOf(1, 2, 3)
    }

    "returns null for blank input" {
        MigrationVersion.parse("").shouldBeNull()
        MigrationVersion.parse("   ").shouldBeNull()
    }

    "returns null when a part is not a number" {
        MigrationVersion.parse("1.a.3").shouldBeNull()
        MigrationVersion.parse("abc").shouldBeNull()
    }

    "next increments the last part of a single-part version" {
        MigrationVersion.parse("3")!!.next() shouldBe MigrationVersion.parse("4")
    }

    "next increments only the last part of a multi-part version" {
        MigrationVersion.parse("1.2.3")!!.next() shouldBe MigrationVersion.parse("1.2.4")
    }

    "V1 and V1.0 are equal (trailing zeros ignored)" {
        MigrationVersion.parse("1") shouldBe MigrationVersion.parse("1.0")
    }

    "V1.0.0 and V1 are equal" {
        MigrationVersion.parse("1.0.0") shouldBe MigrationVersion.parse("1")
    }

    "V1.1 and V1.0 are not equal" {
        (MigrationVersion.parse("1.1") == MigrationVersion.parse("1.0")) shouldBe false
    }

    "V1 is less than V2" {
        MigrationVersion.parse("1")!! shouldBeLessThan MigrationVersion.parse("2")!!
    }

    "V1.9 is less than V2.0" {
        MigrationVersion.parse("1.9")!! shouldBeLessThan MigrationVersion.parse("2.0")!!
    }

    "V10 is greater than V9 (numeric ordering, not lexicographic)" {
        MigrationVersion.parse("10")!! shouldBeGreaterThan MigrationVersion.parse("9")!!
    }

    "V1.2 is less than V1.10" {
        MigrationVersion.parse("1.2")!! shouldBeLessThan MigrationVersion.parse("1.10")!!
    }

    "V1 and V1.0 compare as equal" {
        MigrationVersion.parse("1")!!.compareTo(MigrationVersion.parse("1.0")!!) shouldBe 0
    }

    "toString renders parts joined by dots" {
        MigrationVersion.parse("1.2.3").toString() shouldBe "1.2.3"
    }
})
