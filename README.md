# Flyway Version Tracker

An IntelliJ IDEA plugin that prevents Flyway migration version conflicts across all active remote branches.

---

## What it does

- **Auto-suggests the next safe version** when creating a new migration file (e.g. if `V3__` is the latest, it pre-fills `V4__`)
- **Blocks commits** that would introduce a duplicate migration version already present on any active remote branch
- **Auto-detects migration roots** on project open — works out of the box in monorepos with multiple microservices
- **Sidebar panel** shows per-root migration state and highlights conflicts

---

## Getting started

### 1. Open a project

On project open the plugin automatically:

1. Runs `git fetch` to refresh remote state
2. Scans for Flyway migration files across all tracked paths
3. Registers every discovered migration directory as a **migration root**

You will see a balloon notification: `Flyway Tracker: auto-detected N migration root(s)`.

If auto-detection misses a directory (uncommon), see [Manual configuration](#manual-configuration) below.

### 2. Create a new migration

Right-click any directory inside a migration root in the Project panel:

**New → New Migration**

A dialog appears with the version field pre-filled to the next safe version across all active branches:

```
Version:     V4__          ← auto-filled, edit if needed
Description: add_payment_table
Extension:   sql
```

Click **OK** — the file is created and opened in the editor.

> The plugin checks local files **and** all remote branches that had activity in the last 30 days. It will not suggest a version that is already in use anywhere.

### 3. Commit protection

When you commit (via the IntelliJ commit dialog), the plugin scans staged files. If any staged migration version conflicts with a version already on a remote branch, the commit is blocked:

```
⚠ Migration conflict detected

  V4__add_users.sql conflicts with origin/feature-xyz

Suggested next version: V5__

[Abort Commit]  [Proceed Anyway]
```

Click **Abort Commit** to rename your file, or **Proceed Anyway** if you are intentionally replacing a remote migration.

### 4. Sidebar panel

Open **View → Tool Windows → Flyway Tracker** (or click the icon in the right sidebar).

The panel shows one section per migration root:

```
user-service  —  next: V5__  ⚠ 1 conflict(s)
  Path: services/user-service/src/main/resources/db/migration
  V1__ init_schema.sql
  V2__ add_users.sql
  V3__ add_roles.sql
  V4__ add_payment.sql  ⚠ conflict
```

Click **⟳ Refresh** to re-run `git fetch` and reload.

---

## Manual configuration

### Mark / unmark a migration root

If auto-detection missed a directory:

1. Right-click the directory in the Project panel
2. **Mark Directory As → Migrations Root**

To remove it: right-click → **Unmark as Migrations Root**.

### Pin to a specific branch

By default the plugin checks **all remote branches active in the last 30 days**. To limit checks to one branch:

**Tools → Flyway Tracker → Configure Sync Branch**

Select a branch from the list. Leave it empty to revert to checking all active branches.

### Refresh manually

**Tools → Flyway Tracker → Refresh Migrations** re-fetches remote state and reloads the sidebar.

---

## How active branches are determined

A branch is considered active if its last commit was within the past **30 days**. This is evaluated via:

```
git for-each-ref refs/remotes/ --format=%(refname:short)|%(committerdate:unix)
```

Stale branches are ignored automatically — no configuration needed.

---

## Monorepo support

Each migration root is tracked independently. If your project has multiple microservices with separate migration directories, each will appear as its own section in the sidebar with its own version sequence and conflict state.

```
order-service  —  next: V3__
payment-service  —  next: V7__  ⚠ 1 conflict(s)
user-service  —  next: V5__
```

Settings are stored per-project in `.idea/flyway-tracker.xml` (committed to git).
