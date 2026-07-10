package com.aymaneelmaini.flywaytracker.actions

import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class UnMarkAsMigrationRootAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val settings = e.project?.let { ProjectSettings.getInstance(it) }
        val root = settings?.migrationRootPath ?: ""
        e.presentation.isEnabledAndVisible = e.project != null
            && file != null
            && file.isDirectory
            && root.isNotBlank()
            && file.path.endsWith(root)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProjectSettings.getInstance(project).migrationRootPath = ""
    }
}
