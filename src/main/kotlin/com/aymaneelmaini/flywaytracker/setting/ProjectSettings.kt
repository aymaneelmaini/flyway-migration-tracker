package com.aymaneelmaini.flywaytracker.setting

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@State(
    name = "FlywayVersionTrackerSettings",
    storages = [Storage("flyway-tracker.xml")]
)
@Service(Service.Level.PROJECT)
class ProjectSettings : PersistentStateComponent<ProjectSettings.State> {

    class State {
        @JvmField var migrationRootPaths: MutableList<String> = mutableListOf()
        @JvmField var syncBranch: String = ""
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var migrationRootPaths: MutableList<String>
        get() = myState.migrationRootPaths
        set(value) { myState.migrationRootPaths = value }

    var syncBranch: String
        get() = myState.syncBranch
        set(value) { myState.syncBranch = value }

    val isConfigured: Boolean
        get() = migrationRootPaths.isNotEmpty()

    fun addRoot(path: String) {
        if (path.isNotBlank() && !migrationRootPaths.contains(path)) {
            migrationRootPaths.add(path)
        }
    }

    fun removeRoot(path: String) {
        migrationRootPaths.remove(path)
    }

    companion object {
        fun getInstance(project: Project): ProjectSettings =
            project.getService(ProjectSettings::class.java)
    }
}
