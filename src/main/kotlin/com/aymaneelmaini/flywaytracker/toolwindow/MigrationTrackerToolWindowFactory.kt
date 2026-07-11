package com.aymaneelmaini.flywaytracker.toolwindow

import com.aymaneelmaini.flywaytracker.entity.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.service.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.setting.ProjectSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

class MigrationTrackerToolWindowFactory : ToolWindowFactory {

    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = MigrationTrackerPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.root, null, false)
        toolWindow.contentManager.addContent(content)
    }

    private class MigrationTrackerPanel(private val project: Project) {

        val root: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(buildTopBar(), BorderLayout.NORTH)
            add(buildScrollableContent(), BorderLayout.CENTER)
        }

        private lateinit var contentPanel: JBPanel<*>

        private fun buildTopBar(): JComponent =
            JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(6, 8)
                add(JBLabel("<html><b>Flyway Version Tracker</b></html>"), BorderLayout.WEST)
                add(JButton("⟳ Refresh").apply { addActionListener { refresh() } }, BorderLayout.EAST)
            }

        private fun buildScrollableContent(): JComponent {
            contentPanel = JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(4, 8)
            }
            buildContent()
            return JBScrollPane(contentPanel)
        }

        private fun refresh() {
            FlywayMigrationService.getInstance(project).refresh()
            contentPanel.removeAll()
            buildContent()
            contentPanel.revalidate()
            contentPanel.repaint()
        }

        private fun buildContent() {
            val settings = ProjectSettings.getInstance(project)
            val service = FlywayMigrationService.getInstance(project)

            if (!settings.isConfigured) {
                contentPanel.add(warn("No migration roots configured.\nWill auto-detect on next project open,\nor right-click a folder → Mark as Migrations Root"))
                return
            }

            val branchLabel = settings.syncBranch.ifBlank { "all active branches (last 30 days)" }
            contentPanel.add(info("Checking: $branchLabel"))
            contentPanel.add(Box.createVerticalStrut(6))

            settings.migrationRootPaths.forEach { root ->
                val local = service.getLocalMigrations(root)
                val remote = service.getRemoteMigrations(root)
                val next = service.nextVersionFrom(local + remote)
                val conflictCount = local.count { m -> remote.any { it.conflictsWith(m) } }
                val label = root.substringAfterLast("/").ifBlank { root }
                val conflictSuffix = if (conflictCount > 0) "  ⚠ $conflictCount conflict(s)" else ""

                contentPanel.add(sectionLabel("$label  —  next: $next$conflictSuffix"))
                contentPanel.add(info("  Path: $root"))

                if (local.isEmpty()) {
                    contentPanel.add(info("  (no local migrations)"))
                } else {
                    local.sortedBy { it.version }.forEach { m ->
                        contentPanel.add(migrationRow(m, isConflict = remote.any { it.conflictsWith(m) }))
                    }
                }
                contentPanel.add(Box.createVerticalStrut(10))
            }
        }

        private fun info(text: String): JComponent = JBLabel(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(2)
        }

        private fun warn(text: String): JComponent =
            JBLabel("<html>${text.replace("\n", "<br>")}</html>").apply {
                foreground = JBUI.CurrentTheme.Label.disabledForeground()
                alignmentX = Component.LEFT_ALIGNMENT
            }

        private fun sectionLabel(text: String): JComponent =
            JBLabel("<html><b>$text</b></html>").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                border = JBUI.Borders.emptyBottom(2)
            }

        private fun migrationRow(m: FlywayMigrationFile, isConflict: Boolean): JComponent {
            val suffix = if (isConflict) " <font color='red'>⚠ conflict</font>" else ""
            return JBLabel("<html>  ${m.prefix}${m.version}__ ${m.description}.${m.extension}$suffix</html>").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                border = JBUI.Borders.emptyBottom(2)
            }
        }
    }
}
