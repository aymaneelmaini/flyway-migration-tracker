package com.aymaneelmaini.flywaytracker.actions

import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
import com.aymaneelmaini.flywaytracker.utils.relativePathIn
import com.intellij.openapi.actionSystem.*

class MarkAsMigrationRootAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = false
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isDirectory } ?: return
        e.presentation.isEnabledAndVisible =
            !ProjectSettings.getInstance(project).migrationRootPaths.contains(file.relativePathIn(project))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        ProjectSettings.getInstance(project).addRoot(file.relativePathIn(project))
    }
}
