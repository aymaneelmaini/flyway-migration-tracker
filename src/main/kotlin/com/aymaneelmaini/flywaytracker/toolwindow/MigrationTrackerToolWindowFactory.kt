package com.aymaneelmaini.flywaytracker.toolwindow

import com.aymaneelmaini.flywaytracker.entities.FlywayMigrationFile
import com.aymaneelmaini.flywaytracker.services.FlywayMigrationService
import com.aymaneelmaini.flywaytracker.settings.ProjectSettings
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

    class MigrationTrackerPanel(private val project: Project) {

        val root: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(buildTopBar(), BorderLayout.NORTH)
            add(buildScrollableContent(), BorderLayout.CENTER)
        }

        private lateinit var contentPanel: JBPanel<*>

        private fun buildTopBar(): JComponent {
            val bar = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                border = JBUI.Borders.empty(6, 8)
            }
            val title = JBLabel("<html><b>Flyway Version Tracker</b></html>")
            val refreshBtn = JButton("⟳ Refresh").apply {
                addActionListener { refresh() }
            }
            bar.add(title, BorderLayout.WEST)
            bar.add(refreshBtn, BorderLayout.EAST)
            return bar
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
                contentPanel.add(warn("No migration root configured.\nRight-click a folder → Mark as Migrations Root"))
                return
            }

            contentPanel.add(info("Migration Root: ${settings.migrationRootPath}"))
            contentPanel.add(info("Sync Branch: ${settings.syncBranch.ifBlank { "(not configured)" }}"))
            contentPanel.add(Box.createVerticalStrut(8))

            val local = service.getLocalMigrations()
            val remote = service.getRemoteMigrations()

            contentPanel.add(sectionLabel("Local Migrations (${local.size})"))
            if (local.isEmpty()) {
                contentPanel.add(info("  (none)"))
            } else {
                local.sortedWith(Comparator { a, b ->
                    com.aymaneelmaini.flywaytracker.entities.MigrationVersionComparator.compare(a.version, b.version)
                }).forEach { m ->
                    val isConflict = remote.any { it.conflictsWith(m) }
                    contentPanel.add(migrationRow(m, isConflict))
                }
            }

            contentPanel.add(Box.createVerticalStrut(8))

            if (settings.syncBranch.isNotBlank()) {
                contentPanel.add(sectionLabel("Remote (${settings.syncBranch}) — ${remote.size} migrations"))
                if (remote.isEmpty()) {
                    contentPanel.add(info("  (none or branch not fetched)"))
                } else {
                    remote.sortedWith(Comparator { a, b ->
                        com.aymaneelmaini.flywaytracker.entities.MigrationVersionComparator.compare(a.version, b.version)
                    }).forEach { m -> contentPanel.add(info("  ${m.prefix}${m.version}__ — ${m.description}.${m.extension}")) }
                }
                contentPanel.add(Box.createVerticalStrut(8))
            }

            val next = service.getNextVersion()
            contentPanel.add(sectionLabel("Next safe version: $next"))
        }

        private fun info(text: String): JComponent = JBLabel(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(2)
        }

        private fun warn(text: String): JComponent = JBLabel("<html>${text.replace("\n", "<br>")}</html>").apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            alignmentX = Component.LEFT_ALIGNMENT
        }

        private fun sectionLabel(text: String): JComponent = JBLabel("<html><b>$text</b></html>").apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(4)
        }

        private fun migrationRow(m: FlywayMigrationFile, conflict: Boolean): JComponent {
            val marker = if (conflict) " <font color='red'>⚠ CONFLICT with remote</font>" else ""
            return JBLabel("<html>  ${m.prefix}${m.version}__ — ${m.description}.${m.extension}$marker</html>").apply {
                alignmentX = Component.LEFT_ALIGNMENT
                border = JBUI.Borders.emptyBottom(2)
            }
        }
    }
}
