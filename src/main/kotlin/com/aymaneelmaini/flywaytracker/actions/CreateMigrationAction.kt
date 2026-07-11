package com.aymaneelmaini.flywaytracker.actions

import com.aymaneelmaini.flywaytracker.entities.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.services.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages

class CreateMigrationAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run { e.presentation.isEnabledAndVisible = false; return }
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: run { e.presentation.isEnabledAndVisible = false; return }
        val service = FlywayMigrationService.getInstance(project)
        val filePath = if (file.isDirectory) file.path else file.parent?.path ?: ""
        e.presentation.isEnabledAndVisible = ProjectSettings.getInstance(project).isConfigured
            && service.findRootFor(filePath) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = FlywayMigrationService.getInstance(project)

        val filePath = if (file.isDirectory) file.path else file.parent?.path ?: ""
        val root = service.findRootFor(filePath) ?: run {
            Messages.showErrorDialog(project, "Current directory is not inside a configured migration root.", "Flyway Tracker")
            return
        }

        val nextVersion = service.getNextVersion(root)
        val suggested = "${nextVersion}describe_your_migration.sql"

        val input = Messages.showInputDialog(
            project,
            "Migration root: $root\nNext suggested version: $nextVersion",
            "New Flyway Migration",
            null,
            suggested,
            null
        ) ?: return

        val migration = FlywayMigrationFile.from(input)
        if (migration == null) {
            Messages.showErrorDialog(
                project,
                "Invalid migration name: '$input'\nExpected: V<version>__<description>.<ext>  (e.g. V3__add_users.sql)",
                "Invalid Migration Name"
            )
            return
        }

        if (service.isVersionUsed(migration.version, root, migration.prefix)) {
            Messages.showErrorDialog(
                project,
                "${migration.prefix}${migration.version}__ already used in $root.\nNext available: ${service.getNextVersion(root)}",
                "Migration Version Conflict"
            )
            return
        }

        val targetDir = if (file.isDirectory) file else file.parent
        WriteCommandAction.runWriteCommandAction(project) {
            val newFile = targetDir.createChildData(this, input)
            FileEditorManager.getInstance(project).openFile(newFile, true)
        }
    }
}
