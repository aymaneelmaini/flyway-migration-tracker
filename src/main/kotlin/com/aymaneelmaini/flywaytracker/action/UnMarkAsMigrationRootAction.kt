package com.aymaneelmaini.flywaytracker.action

import com.aymaneelmaini.flywaytracker.setting.ProjectSettings
import com.aymaneelmaini.flywaytracker.util.relativePathIn
import com.intellij.openapi.actionSystem.*

class UnMarkAsMigrationRootAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = false
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf { it.isDirectory } ?: return
        e.presentation.isEnabledAndVisible =
            ProjectSettings.getInstance(project).migrationRootPaths.contains(file.relativePathIn(project))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        ProjectSettings.getInstance(project).removeRoot(file.relativePathIn(project))
    }
}
