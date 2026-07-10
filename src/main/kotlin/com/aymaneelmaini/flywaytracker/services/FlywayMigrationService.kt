package com.aymaneelmaini.flywaytracker.services

import com.aymaneelmaini.flywaytracker.entities.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.entities.MigrationVersion
import com.aymaneelmaini.flywaytracker.entities.MigrationVersionComparator
import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
import com.aymaneelmaini.flywaytracker.utils.TerminalClient
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class FlywayMigrationService(private val project: Project) {

    private val settings get() = ProjectSettings.getInstance(project)
    private val terminal get() = TerminalClient(project.basePath ?: "")

    fun getLocalMigrations(): List<FlywayMigrationFile> {
        val root = settings.migrationRootPath.ifBlank { return emptyList() }
        val tracked = terminal.exec("git", "ls-files", root)
        val untracked = terminal.exec("git", "ls-files", "-o", "--exclude-standard", root)
        return (tracked + untracked)
            .mapNotNull { FlywayMigrationFile.from(it) }
            .distinctBy { "${it.prefix}${it.version}" }
    }

    fun getRemoteMigrations(): List<FlywayMigrationFile> {
        val branch = settings.syncBranch.ifBlank { return emptyList() }
        val root = settings.migrationRootPath.ifBlank { return emptyList() }
        return terminal.exec("git", "ls-tree", "-r", "--name-only", branch, root)
            .mapNotNull { FlywayMigrationFile.from(it) }
    }

    fun getAllMigrations(): List<FlywayMigrationFile> =
        (getLocalMigrations() + getRemoteMigrations())
            .distinctBy { "${it.prefix}${it.version}" }

    fun getNextVersion(): String {
        val versioned = getAllMigrations().filter { it.prefix == 'V' }
        if (versioned.isEmpty()) return "V1__"
        val max = versioned.maxWithOrNull { a, b ->
            MigrationVersionComparator.compare(a.version, b.version)
        }?.version ?: return "V1__"
        return "V${max.next()}__"
    }

    fun isVersionUsed(version: MigrationVersion, prefix: Char = 'V'): Boolean =
        getAllMigrations().any { it.prefix == prefix && it.version == version }

    fun isVersionUsedOnRemote(version: MigrationVersion, prefix: Char = 'V'): Boolean =
        getRemoteMigrations().any { it.prefix == prefix && it.version == version }

    fun getRemoteBranches(): List<String> =
        terminal.exec("git", "branch", "-r", "--format=%(refname:short)")

    fun refresh() {
        terminal.exec("git", "fetch")
    }

    companion object {
        fun getInstance(project: Project): FlywayMigrationService =
            project.getService(FlywayMigrationService::class.java)
    }
}
