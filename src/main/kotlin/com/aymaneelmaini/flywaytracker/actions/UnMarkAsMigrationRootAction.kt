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
        val rel = e.project?.let { file?.let { f -> relativePath(it, f) } }
        e.presentation.isEnabledAndVisible = e.project != null
            && file != null
            && file.isDirectory
            && rel != null
            && settings?.migrationRootPaths?.contains(rel) == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        ProjectSettings.getInstance(project).removeRoot(relativePath(project, file))
    }

    private fun relativePath(project: Project, file: VirtualFile): String {
        val base = project.basePath ?: return file.path
        return if (file.path.startsWith(base)) file.path.removePrefix(base).trimStart('/') else file.path
    }
}
