package com.aymaneelmaini.flywaytracker.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "FlywayVersionTrackerSettings",
    storages = [Storage("flyway-tracker.xml")]
)
@Service(Service.Level.PROJECT)
class ProjectSettings : PersistentStateComponent<ProjectSettings.State> {

    data class State(
        var migrationRootPath: String = "",
        var syncBranch: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var migrationRootPath: String
        get() = myState.migrationRootPath
        set(value) { myState.migrationRootPath = value }

    var syncBranch: String
        get() = myState.syncBranch
        set(value) { myState.syncBranch = value }

    val isConfigured: Boolean
        get() = migrationRootPath.isNotBlank()

    companion object {
        fun getInstance(project: Project): ProjectSettings =
            project.getService(ProjectSettings::class.java)
    }
}
