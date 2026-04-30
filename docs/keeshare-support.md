# KeeShare Support for KeePassDX

## Overview

KeeShare enables sharing password groups between devices via encrypted container
files. Each shared group is exported to a small KDBX container that can be
synced to other devices through any folder-sync tool. On the receiving side,
entries from the container are merged into the corresponding group.

KeePassDX accesses all sync directories and container files exclusively through
Android's Storage Access Framework (SAF) — `DocumentFile`, `ContentResolver`,
and `content://` tree URIs.

---

## Core Concepts

### Container Files

A **container file** is a miniature KDBX database holding only the entries from
one shared group. It is encrypted with a password separate from the main
database password.

**Export** builds a container from the shared group's entries and writes it via
`ContentResolver.openOutputStream()`.

**Import** opens containers from other devices via
`ContentResolver.openInputStream()`, decrypts them, and merges entries into the
target group using timestamp-based conflict resolution (newer entry wins; losing
version preserved in entry history).

### Per-Device Sync

Each device writes to its own container file (`{DEVICE_ID}.kdbx`) and reads
from all others. This eliminates write conflicts — two devices can export
simultaneously without data loss.

```
Sync Folder/
├── Shared-Passwords/
│   ├── LAPTOP7.kdbx     ← desktop exported this
│   ├── PHONE01.kdbx     ← phone exported this
│   └── TABLET.kdbx      ← tablet exported this
```

### Device Identity

Each device needs a short unique ID. Resolution order:

1. **User-configured ID** — set manually in Settings > KeeShare
2. **Generated fallback** — random 7-character hex ID, persisted in preferences

Device IDs are sanitized to alphanumeric characters only (prevents path
traversal via filenames like `../../etc/passwd` → `etcpasswd.kdbx`).

### Sync Direction

| Type        | Value | Behavior                                |
|-------------|-------|-----------------------------------------|
| Inactive    | 0     | KeeShare configured but disabled        |
| Import      | 1     | Read-only: pull entries from containers  |
| Export      | 2     | Write-only: push entries to containers   |
| Synchronize | 3     | Bidirectional: both import and export    |

### Group Configuration

KeeShare configuration is stored in group-level KDBX `CustomData`:

**Classic reference** (`KeeShare/Reference`): base64-encoded XML with type,
path, password, and group UUID. Read for import compatibility.

**Per-device config** (`KeeShare/PerDeviceSync`): XML with sync directory URI
(`content://...`), password, and keepGroups flag. Used for all SAF-based
import/export.

### Stale Device Cleanup

Container files not modified in 90 days (configurable) are automatically
deleted via `DocumentFile.delete()`. A device's own container is never deleted.

---

## SAF I/O Patterns

All file access uses Android's Storage Access Framework. No `java.io.File`,
`FileInputStream`, or filesystem paths are used for sync directory access.

**Reading containers:**
```
DocumentFile.fromTreeUri(context, treeUri)
  → dir.listFiles().filter { .kdbx files from other devices }
  → contentResolver.openInputStream(doc.uri)
  → KeeShareContainer.read(inputStream, password, cacheDir)
```

**Writing containers:**
```
DocumentFile.fromTreeUri(context, treeUri)
  → dir.findFile(fileName) ?: dir.createFile("application/octet-stream", fileName)
  → contentResolver.openOutputStream(targetDoc.uri)
  → KeeShareContainer.writeUnsigned(database, outputStream, password)
```

**Change detection:**
```
DocumentFile.lastModified() > lastSyncTimestamp
```

**Cleanup:**
```
DocumentFile.delete() for containers older than staleDays threshold
```

---

## Architecture

### Layer Diagram

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  GroupActivity  ·  EntryActivity  ·  Settings        │
│  (menu items, toasts, visual indicators)             │
├─────────────────────────────────────────────────────┤
│                 ViewModel Layer                       │
│  DatabaseViewModel.syncKeeShare()                    │
├─────────────────────────────────────────────────────┤
│                 Service Layer                         │
│  DatabaseTaskNotificationService                     │
│  (intent dispatch, delegates to KeeShareSyncManager) │
├─────────────────────────────────────────────────────┤
│              Sync Orchestration Layer                 │
│  KeeShareSyncManager (periodic timer, export-on-save,│
│  stale cleanup via DocumentFile)                     │
├─────────────────────────────────────────────────────┤
│                 Action Layer                          │
│  KeeShareSyncRunnable (extends SaveDatabaseRunnable) │
│  (SAF bridge: DocumentFile + ContentResolver streams) │
├─────────────────────────────────────────────────────┤
│                 Core Layer                            │
│  KeeShareImport  ·  KeeShareExport                   │
│  KeeShareContainer  ·  KeeShareReference             │
│  PerDeviceSyncConfig  ·  DeviceIdentity              │
│  (protocol logic, stream-based container I/O, merge) │
└─────────────────────────────────────────────────────┘
```

### Manual Sync Flow

```
GroupActivity menu > "Sync KeeShare"
  → DatabaseViewModel.syncKeeShare(save=true)
    → DatabaseTaskNotificationService (ACTION_DATABASE_KEESHARE_SYNC_TASK)
      → KeeShareSyncRunnable.onActionRun()
        1. Resolve device ID (preferences → fallback)
        2. KeeShareImport.importAll()
           - Walk groups for KeeShare config
           - Open containers via ContentResolver.openInputStream()
           - Scoped merge into target groups
        3. KeeShareExport.exportAll()
           - Build container databases
           - Write via ContentResolver.openOutputStream()
        4. Save database
        5. Return result bundle (import/export counts)
      → Toast with summary → reload activity
```

### Auto-Sync Flow

```
KeeShareSyncManager periodic timer (every 15 min)
  → hasNewerContainerFiles() per sync dir URI
    → DocumentFile.fromTreeUri() → listFiles() → lastModified()
  → If newer: trigger full sync pipeline
  → cleanupStaleDeviceFiles() per sync dir
    → DocumentFile.delete() for old containers
```

### Export-on-Save

After every database save, `KeeShareSyncManager.triggerExportAfterSave()`
runs a lightweight export of all groups with per-device config, writing
containers via SAF streams.

### Scoped Merge

Entries from a container are merged into a specific target group (not the
entire database) via `DatabaseKDBXMerger.mergeIntoGroup()`:

- Newer entry wins (timestamp-based)
- Entry history preserved
- New entries added, deleted entries tracked
- Custom data and icons carried over

---

## File Inventory

### Core (database module)

| File | Purpose |
|------|---------|
| `KeeShareReference.kt` | Parse/serialize classic references (base64 XML) |
| `PerDeviceSyncConfig.kt` | Per-device config: XML serialization, `containerFileName()` |
| `DeviceIdentity.kt` | `generateFallbackDeviceId()` (7-char hex) |
| `KeeShareImport.kt` | Import orchestration across all groups (stream-based lambdas) |
| `KeeShareExport.kt` | Export orchestration (stream-based `targetStreamProvider` lambda) |
| `KeeShareContainer.kt` | Container read/write via streams, format detection |

### App layer

| File | Purpose |
|------|---------|
| `KeeShareSyncRunnable.kt` | SAF bridge: `listOtherDeviceStreams()`, `openTargetOutputStream()` |
| `KeeShareSyncManager.kt` | Sync orchestration: periodic timer, export-on-save, stale cleanup |
| `preferences_keeshare.xml` | Settings screen (device ID only) |

### UI integration (modified existing files)

| File | Change |
|------|--------|
| `DatabaseTaskNotificationService.kt` | Delegates to `KeeShareSyncManager` for lifecycle |
| `GroupActivity.kt` | "Sync KeeShare" menu item |
| `DatabaseLockActivity.kt` | Sync result toast |
| `DatabaseViewModel.kt` | `syncKeeShare()` bridge |
| `NodesAdapter.kt` | Share indicator icon on groups |
| `PreferencesUtil.kt` | KeeShare preference accessors |

---

## Security

### Container Encryption

Each container is a fully encrypted KDBX database. The reference password
(stored in group custom data, protected by the main database master password)
is required to decrypt.

### Device ID Sanitization

Device IDs are stripped to `[A-Z0-9]` before use as filenames, preventing
path traversal attacks.

### Signed Containers

KeePassDX can read signed containers (`.kdbx.share` ZIP format) but does
not yet verify RSA-2048/SHA-256 signatures. Planned for a future release.

### Trust Model

Security depends on:
1. Strength of the reference password
2. Security of the sync transport (device-to-device)
3. Trustworthiness of devices in the sync group

---

## Troubleshooting

**"Sync KeeShare" menu missing** — Requires KDBX format, non-read-only mode,
group list view (not entry view).

**"Imported 0 entries"** — No container files from other devices in sync
directory, password mismatch, or sync tool hasn't transferred files yet.

**Device ID changed** — Happens if app data is cleared. Old container cleaned
up automatically after 90 days. Set a manual ID in Settings > KeeShare to
prevent.

**Sync not triggering automatically** — Periodic check runs every 15 minutes.
Use manual "Sync KeeShare" for immediate sync.

---

## Future Work

| Feature | Description |
|---------|-------------|
| Sync directory picker UI | SAF `ACTION_OPEN_DOCUMENT_TREE` picker to configure per-device sync directories from within KeePassDX |
| Group configuration UI | Dialog to set up KeeShare on a group without needing desktop tools |
| Signed container verification | RSA-2048/SHA-256 signature verification for `.kdbx.share` files |
| Conflict resolution UI | Manual merge dialog for timestamp-tie conflicts |
