<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Flyway-version-tracker Changelog

## Unreleased

## 1.0.0 - 2026-07-11

### Added

- Flyway version conflict detection — blocks commits with duplicate migration versions
- Tool window showing local vs remote migrations with conflict highlights
- Auto-suggested next migration version when creating a new migration file
- Multi-root support with auto-detection of all migration roots in the project
- `CreateMigrationAction` for generating migration files with version validation
- `MarkAsMigrationRootAction` / `UnMarkAsMigrationRootAction` for managing roots
- `ConfigureSyncBranchAction` listing available remote branches
- `RefreshMigrationsAction` triggering a git fetch
- `MigrationVersionCheckinHandler` blocking commits on duplicate versions
- Persistent project settings (migration root + sync branch)
- Tag-triggered CI release pipeline publishing plugin ZIP to GitHub Releases
