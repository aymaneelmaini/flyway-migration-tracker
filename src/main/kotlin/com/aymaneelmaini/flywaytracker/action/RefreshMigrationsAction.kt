package com.aymaneelmaini.flywaytracker.action

import com.aymaneelmaini.flywaytracker.service.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.util.Notifier
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class RefreshMigrationsAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching remote migrations...", false) {
            override fun run(indicator: ProgressIndicator) {
                FlywayMigrationService.getInstance(project).refresh()
                Notifier.info(project, "Flyway Tracker: remote migrations refreshed.")
            }
        })
    }
}
