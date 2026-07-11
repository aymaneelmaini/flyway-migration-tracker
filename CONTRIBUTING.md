# Contributing to Flyway Version Tracker

This document covers the plugin architecture, how to set up a development environment, and where to extend the plugin.

---

## Architecture overview

```
src/main/kotlin/com/aymaneelmaini/flywaytracker/
├── entity/
│   ├── MigrationVersion.kt            # Version parsing and comparison
│   └── FlywayMigrationFile.kt         # Migration filename parsing
├── setting/
│   └── ProjectSettings.kt             # Persistent per-project state
├── util/
│   ├── TerminalClient.kt              # Git command executor
│   └── Notifier.kt                    # Balloon notifications
│   └── VfsUtils.kt                    # VirtualFile extensions
├── service/
│   └── FlywayMigrationService.kt      # Core business logic
├── action/
│   ├── CreateMigrationAction.kt        # New → New Migration
│   ├── MarkAsMigrationRootAction.kt
│   ├── UnMarkAsMigrationRootAction.kt
│   ├── ConfigureSyncBranchAction.kt
│   └── RefreshMigrationsAction.kt
├── checkin/
│   └── MigrationVersionCheckinHandler.kt  # Pre-commit blocking
├── startup/
│   └── FlywayTrackerStartupActivity.kt    # Runs on project open
└── toolwindow/
    └── MigrationTrackerToolWindowFactory.kt
```

### Data flow

```
Project open
  └── FlywayTrackerStartupActivity
        ├── service.refresh()            → git fetch
        └── service.detectMigrationRoots() → git ls-files → settings.addRoot()

New Migration action
  └── CreateMigrationAction
        ├── service.getLocalMigrations(root)
        ├── service.getRemoteMigrations(root)
        └── service.nextVersionFrom(combined)   → pre-fills dialog

Commit dialog
  └── MigrationVersionCheckinHandler
        ├── resolves root per staged file via service.findRootFor()
        ├── service.getRemoteMigrations(root)
        └── shows conflict dialog or lets commit proceed

Sidebar panel
  └── MigrationTrackerToolWindowFactory
        ├── per root: getLocalMigrations + getRemoteMigrations
        └── renders rows; conflict rows highlighted in red
```

---

## Key classes

### `MigrationVersion`

Parses Flyway version strings (`"3"`, `"1.2.3"`, `"1_2_3"`). Implements `Comparable<MigrationVersion>` so lists can be sorted with `sortedBy { it.version }` and max found with `maxByOrNull { it.version }`. Equality ignores trailing zeros: `V1 == V1.0.0`.

### `FlywayMigrationFile`

Parses migration filenames. `from(path)` strips the directory component so it works with both bare filenames and full paths. `conflictsWith(other)` returns true when prefix and version are both equal (description and extension are irrelevant for conflicts).

### `FlywayMigrationService`

All git I/O goes through this service. It is a project-scoped `@Service` — fetch it anywhere via `FlywayMigrationService.getInstance(project)`.

Key methods:

| Method | Git commands used |
|--------|-------------------|
| `getLocalMigrations(root)` | `git ls-files <root>`, `git ls-files -o --exclude-standard <root>` |
| `getRemoteMigrations(root)` | `git ls-tree -r --name-only <branch> <root>` (one call per active branch) |
| `detectMigrationRoots()` | `git ls-files` |
| `getActiveBranches()` | `git for-each-ref refs/remotes/ --format=%(refname:short)|%(committerdate:unix)` |
| `refresh()` | `git fetch` |

`nextVersionFrom(list)` is a pure function — it takes an already-fetched list and avoids extra git calls. Prefer it over `getNextVersion(root)` when you already have the migrations loaded.

### `ProjectSettings`

Backed by `PersistentStateComponent`, serialised to `.idea/flyway-tracker.xml`. Two fields:

- `migrationRootPaths: MutableList<String>` — relative paths from project root
- `syncBranch: String` — empty means "all active branches"

### `TerminalClient`

Thin wrapper around `ProcessBuilder`. Runs from `project.basePath`. Returns `List<String>` (stdout lines, stderr is suppressed). Every git call goes through here — there is no other I/O in the plugin.

---

## Development setup

**Requirements**

- JDK 21
- IntelliJ IDEA 2025.x (Community or Ultimate)

**Run the plugin**

```bash
./gradlew runIde
```

A sandbox IDE instance opens with the plugin loaded. Open any git project with Flyway migrations to test.

**Run tests**

```bash
./gradlew test
```

Tests use [Kotest](https://kotest.io/) `DescribeSpec` style and are pure unit tests — no IntelliJ platform or git required.

**Build a distributable**

```bash
./gradlew buildPlugin
```

Output: `build/distributions/flyway-version-tracker-<version>.zip`

---

## Project configuration

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Gradle build, platform version, Kotest dependencies |
| `gradle/libs.versions.toml` | Version catalog (Kotlin, Kotest, etc.) |
| `gradle.properties` | Plugin version, group, IntelliJ platform type |
| `src/main/resources/META-INF/plugin.xml` | Extension points, action registrations |

The target platform is defined in `build.gradle.kts`:

```kotlin
intellijPlatform {
    intellijIdea("2025.3.5")
}
```

Change this to test against a different IDE version.

---

## How to add a new feature

### Add a new action

1. Create a class in `action/` extending `AnAction`
2. Override `actionPerformed` (and optionally `update` to control visibility)
3. Register it in `plugin.xml` under the relevant `<group-id>`

### Add a new git query

1. Add a method to `FlywayMigrationService` using `terminal.exec(...)`
2. Parse the output into domain types from `entity/`
3. Keep all git logic in the service — actions and the tool window only call service methods

### Add a new persistent setting

1. Add a `@JvmField` to `ProjectSettings.State`
2. Access it via `ProjectSettings.getInstance(project).yourField`
3. The value is automatically persisted to `.idea/flyway-tracker.xml`

### Write tests

All entity and pure-function logic is testable without the platform. Add specs to:

```
src/test/kotlin/com/aymaneelmaini/flywaytracker/
├── entity/
│   ├── MigrationVersionSpec.kt
│   └── FlywayMigrationFileSpec.kt
└── service/
    └── FlywayMigrationServicePureSpec.kt   ← pure functions only
```

For service tests that need git, pass a fake/stub `TerminalClient` — it is constructed from a working-directory string so you can inject a test double by subclassing.

---

## Extension points used

| Extension point | Class | Purpose |
|----------------|-------|---------|
| `projectService` | `ProjectSettings`, `FlywayMigrationService` | Project-scoped singletons |
| `projectActivity` | `FlywayTrackerStartupActivity` | Runs on project open (coroutine-based) |
| `toolWindow` | `MigrationTrackerToolWindowFactory` | Sidebar panel |
| `checkinHandlerFactory` | `MigrationVersionCheckinHandlerFactory` | Pre-commit blocking |
| `notificationGroup` | — | Balloon notifications |

Actions are registered in `plugin.xml` under:

- `NewGroup` — New Migration (New File menu)
- `MarkRootGroup` — Mark/Unmark as Migrations Root (directory context menu)
- `ToolsMenu` → `flyway.tracker.toolsGroup` — Configure Sync Branch, Refresh Migrations
