package com.aymaneelmaini.flywaytracker.service

import com.aymaneelmaini.flywaytracker.entity.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.entity.MigrationVersion
import com.aymaneelmaini.flywaytracker.setting.ProjectSettings
import com.aymaneelmaini.flywaytracker.util.TerminalClient
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

private const val ACTIVE_BRANCH_DAYS = 30L
private val EXCLUDED_DIRS = setOf("build", "target", ".gradle", ".idea", "node_modules", ".git", "out", "dist")
private val DEFAULT_BRANCH_CANDIDATES = listOf("origin/main", "origin/master", "origin/develop", "origin/dev")
private val MIGRATION_FILE_REGEX = Regex(".+/[VUR]\\d[^/]*__[^/]+\\.[a-zA-Z]+$")

@Service(Service.Level.PROJECT)
class FlywayMigrationService(private val project: Project) {

    private val settings get() = ProjectSettings.getInstance(project)
    private val terminal get() = TerminalClient(project.basePath ?: "")

    // ── per-root queries ──────────────────────────────────────────────────────

    fun getLocalMigrations(root: String): List<FlywayMigrationFile> {
        val tracked = terminal.exec("git", "ls-files", root)
        val untracked = terminal.exec("git", "ls-files", "-o", "--exclude-standard", root)
        return (tracked + untracked)
            .mapNotNull { FlywayMigrationFile.from(it) }
            .distinctBy { it.prefix to it.version }
    }

    fun getRemoteMigrations(root: String): List<FlywayMigrationFile> {
        val branches = if (settings.syncBranch.isNotBlank()) listOf(settings.syncBranch) else getActiveBranches()
        return branches
            .flatMap { branch ->
                terminal.exec("git", "ls-tree", "-r", "--name-only", branch, root)
                    .mapNotNull { FlywayMigrationFile.from(it) }
            }
            .distinctBy { it.prefix to it.version }
    }

    fun getNextVersion(root: String): String =
        nextVersionFrom(getLocalMigrations(root) + getRemoteMigrations(root))

    /** Compute next version from an already-fetched list — avoids redundant git calls. */
    fun nextVersionFrom(migrations: List<FlywayMigrationFile>): String {
        val max = migrations.filter { it.prefix == 'V' }.maxByOrNull { it.version }?.version
        return if (max != null) "V${max.next()}__" else "V1__"
    }

    fun isVersionUsed(version: MigrationVersion, root: String, prefix: Char = 'V'): Boolean =
        (getLocalMigrations(root) + getRemoteMigrations(root)).any { it.prefix == prefix && it.version == version }

    fun isVersionUsedOnRemote(version: MigrationVersion, root: String, prefix: Char = 'V'): Boolean =
        getRemoteMigrations(root).any { it.prefix == prefix && it.version == version }

    // ── multi-root helpers ────────────────────────────────────────────────────

    fun findRootFor(filePath: String): String? {
        val normalized = filePath.replace("\\", "/")
        return settings.migrationRootPaths.firstOrNull { normalized.contains(it.replace("\\", "/")) }
    }

    // ── discovery ─────────────────────────────────────────────────────────────

    fun detectMigrationRoots(): List<String> =
        terminal.exec("git", "ls-files")
            .filter { path -> MIGRATION_FILE_REGEX.matches("/$path") }
            .filter { path -> EXCLUDED_DIRS.none { exc -> path.split("/").contains(exc) } }
            .map { it.substringBeforeLast("/") }
            .filter { it.isNotBlank() }
            .distinct()

    fun getActiveBranches(withinDays: Long = ACTIVE_BRANCH_DAYS): List<String> {
        val cutoff = (System.currentTimeMillis() / 1000L) - (withinDays * 86_400L)
        return terminal.exec(
            "git", "for-each-ref",
            "--sort=-committerdate",
            "refs/remotes/",
            "--format=%(refname:short)|%(committerdate:unix)"
        ).mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 2) return@mapNotNull null
            val name = parts[0].trim()
            val epoch = parts[1].trim().toLongOrNull() ?: return@mapNotNull null
            name.takeIf { epoch >= cutoff && !it.endsWith("/HEAD") }
        }
    }

    fun detectDefaultBranch(): String? {
        val active = getActiveBranches(365)
        return DEFAULT_BRANCH_CANDIDATES.firstOrNull { it in active }
            ?: active.firstOrNull { it.startsWith("origin/") }
    }

    fun refresh() {
        terminal.exec("git", "fetch")
    }

    companion object {
        fun getInstance(project: Project): FlywayMigrationService =
            project.getService(FlywayMigrationService::class.java)
    }
}
