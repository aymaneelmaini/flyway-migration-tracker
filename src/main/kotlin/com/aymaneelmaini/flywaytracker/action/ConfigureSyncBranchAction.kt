package com.aymaneelmaini.flywaytracker.action

import com.aymaneelmaini.flywaytracker.service.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.setting.ProjectSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.Messages

class ConfigureSyncBranchAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = FlywayMigrationService.getInstance(project)
        val branches = service.getActiveBranches()

        if (branches.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "No active remote branches found (updated within last 30 days).\nEnsure the project has a git remote and try 'Refresh Migrations' first.",
                "Flyway Tracker"
            )
            return
        }

        val settings = ProjectSettings.getInstance(project)
        val current = settings.syncBranch
        val defaultBranch = if (current.isNotBlank() && branches.contains(current)) current else branches[0]

        val choice = Messages.showEditableChooseDialog(
            "Select the remote branch to check for migration version conflicts:",
            "Configure Sync Branch",
            null,
            branches.toTypedArray(),
            defaultBranch,
            null
        ) ?: return

        settings.syncBranch = choice
    }
}
