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
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val root = ProjectSettings.getInstance(project).migrationRootPath
        e.presentation.isEnabledAndVisible = root.isNotBlank()
            && file != null
            && (file.isDirectory && file.path.contains(root) || !file.isDirectory && file.parent?.path?.contains(root) == true)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = FlywayMigrationService.getInstance(project)

        val nextVersion = service.getNextVersion()
        val suggested = "${nextVersion}describe_your_migration.sql"

        val input = Messages.showInputDialog(
            project,
            "Enter migration file name:\n(Next suggested version: $nextVersion)",
            "New Flyway Migration",
            null,
            suggested,
            null
        ) ?: return

        val migration = FlywayMigrationFile.from(input)
        if (migration == null) {
            Messages.showErrorDialog(
                project,
                "Invalid migration name: '$input'\nExpected format: V<version>__<description>.<ext>\nExample: V3__add_users_table.sql",
                "Invalid Migration Name"
            )
            return
        }

        if (service.isVersionUsed(migration.version, migration.prefix)) {
            val next = service.getNextVersion()
            Messages.showErrorDialog(
                project,
                "${migration.prefix}${migration.version}__ is already in use locally or on the sync branch.\n\nNext available version: $next",
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
