package com.aymaneelmaini.flywaytracker.startup

import com.aymaneelmaini.flywaytracker.service.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.setting.ProjectSettings
import com.aymaneelmaini.flywaytracker.util.Notifier
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlywayTrackerStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) = withContext(Dispatchers.IO) {
        val service = FlywayMigrationService.getInstance(project)
        val settings = ProjectSettings.getInstance(project)

        // Fresh remote state first so detection is accurate
        service.refresh()

        // Auto-detect all migration roots if none configured
        if (!settings.isConfigured) {
            val detected = service.detectMigrationRoots()
            if (detected.isNotEmpty()) {
                detected.forEach { settings.addRoot(it) }
                Notifier.info(project, "Flyway Tracker: auto-detected ${detected.size} migration root(s)")
            }
        }

        // syncBranch stays blank → checks all active branches automatically
    }
}
