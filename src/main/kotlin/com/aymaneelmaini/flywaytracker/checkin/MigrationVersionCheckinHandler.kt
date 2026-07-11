package com.aymaneelmaini.flywaytracker.checkin

import com.aymaneelmaini.flywaytracker.entities.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.services.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import com.intellij.util.PairConsumer

class MigrationVersionCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler =
        MigrationVersionCheckinHandler(panel)
}

class MigrationVersionCheckinHandler(
    private val panel: CheckinProjectPanel
) : CheckinHandler() {

    override fun beforeCheckin(
        executor: CommitExecutor?,
        additionalDataConsumer: PairConsumer<Any, Any>?
    ): ReturnResult {
        val project = panel.project
        val settings = ProjectSettings.getInstance(project)
        if (!settings.isConfigured) return ReturnResult.COMMIT

        val service = FlywayMigrationService.getInstance(project)

        // Group staged migration files by their root, then check each root independently
        data class Conflict(val migration: FlywayMigrationFile, val root: String)

        val conflicts = panel.virtualFiles.mapNotNull { vFile ->
            val root = service.findRootFor(vFile.path) ?: return@mapNotNull null
            val migration = FlywayMigrationFile.from(vFile.name) ?: return@mapNotNull null
            if (service.isVersionUsedOnRemote(migration.version, root, migration.prefix))
                Conflict(migration, root)
            else null
        }

        if (conflicts.isEmpty()) return ReturnResult.COMMIT

        val list = conflicts.joinToString("\n") { "  • [${it.root.substringAfterLast('/')}] ${it.migration.rawFileName}" }
        val suggestions = conflicts.distinctBy { it.root }
            .joinToString("\n") { "  ${it.root.substringAfterLast('/')}: ${service.getNextVersion(it.root)}" }

        val choice = Messages.showYesNoDialog(
            project,
            "The following migration(s) conflict with active remote branches:\n\n$list\n\nNext safe versions:\n$suggestions\n\nAbort the commit to fix?",
            "Migration Version Conflict",
            "Abort Commit",
            "Proceed Anyway",
            Messages.getWarningIcon()
        )

        return if (choice == Messages.YES) ReturnResult.CANCEL else ReturnResult.COMMIT
    }
}
