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

        if (!settings.isConfigured || settings.syncBranch.isBlank()) return ReturnResult.COMMIT

        val service = FlywayMigrationService.getInstance(project)
        val root = settings.migrationRootPath

        val conflicting = panel.virtualFiles
            .filter { it.path.contains(root) }
            .mapNotNull { FlywayMigrationFile.from(it.name) }
            .filter { m -> service.isVersionUsedOnRemote(m.version, m.prefix) }

        if (conflicting.isEmpty()) return ReturnResult.COMMIT

        val nextSuggestion = service.getNextVersion()
        val list = conflicting.joinToString("\n") { "  • ${it.rawFileName}" }

        val choice = Messages.showYesNoDialog(
            project,
            "The following migration(s) conflict with branch '${settings.syncBranch}':\n\n$list\n\nNext safe version: $nextSuggestion\n\nAbort the commit to fix the version?",
            "Migration Version Conflict",
            "Abort Commit",
            "Proceed Anyway",
            Messages.getWarningIcon()
        )

        return if (choice == Messages.YES) ReturnResult.CANCEL else ReturnResult.COMMIT
    }
}
