# KeeShare SAF Refactoring

**Branch**: `feature/keeshare-saf-refactor`
**Base**: `feature/keeshare-support`
**Date**: 2026-02-23

---

## Maintainer Review Summary

The original fork's KeeShare implementation had 4 architectural violations:

1. **Direct file I/O** — `java.io.File`, `FileInputStream`, `File.listFiles()` instead of SAF (`ContentResolver` + URI streams)
2. **Network connections** — HTTP calls to Syncthing REST API violate KeePassDX's purely-local design
3. **Syncthing coupling** — Event poller and device ID resolution tied to one sync protocol
4. **Sync logic in global service** — ~170 lines of KeeShare code stuffed into `DatabaseTaskNotificationService`

Kept: data model classes (`KeeShareReference`, `DeviceIdentity` fallback), container read/write streams, merge logic, conflict resolution, unit tests for utility classes.

---

## What Changed

### Deleted

| File | Reason |
|------|--------|
| `SyncthingEventPoller.kt` | Syncthing REST API poller |
| `KeeShareFileObserver.kt` | inotify file watcher (requires filesystem paths) |

### Stripped

| File | What was removed |
|------|-----------------|
| `DeviceIdentity.kt` | All HTTP code, Syncthing API query, JSON parsing. Kept `generateFallbackDeviceId()` only. |
| `PerDeviceSyncConfig.kt` | `listOtherDeviceFiles()`, `listAllDeviceFiles()`, `cleanupStaleDeviceFiles()`, `autoUpgradeClassicReferences()` — all used `java.io.File`. Kept XML serialization and `containerFileName()`. |
| `KeeShareContainer.kt` | `writeUnsignedAtomic()` (filesystem temp-file + rename). Kept stream-based `read()` and `writeUnsigned()`. |

### Changed

| File | Change |
|------|--------|
| `KeeShareExport.kt` | `targetFileProvider: (String, String) -> File?` → `targetStreamProvider: (String, String) -> OutputStream?`. Writes via `KeeShareContainer.writeUnsigned()` to SAF stream. |
| `KeeShareSyncRunnable.kt` | Replaced `File`/`FileInputStream` lambdas with SAF: `DocumentFile.fromTreeUri()`, `contentResolver.openInputStream/openOutputStream()`. Removed Syncthing from `resolveDeviceId()` — now preferences → fallback only. |
| `DatabaseTaskNotificationService.kt` | Removed ~170 lines: `startKeeShareAutoSync()`, `stopKeeShareAutoSync()`, `hasNewerContainerFiles()`, `triggerKeeShareExportAfterSave()`, member vars for poller/observer/job. Replaced with delegation to `KeeShareSyncManager`. |
| `PreferencesUtil.kt` | Removed `getKeeShareSyncthingApiUrl()`, `getKeeShareSyncthingApiKey()`. |
| `preferences_keeshare.xml` | Removed Syncthing API URL and API key fields. Kept device ID. |
| String resources | Removed 6 Syncthing-related strings, updated settings summary. |

### New

| File | Purpose |
|------|---------|
| `KeeShareSyncManager.kt` | Extracted sync orchestration: periodic timer (SAF-based `DocumentFile.lastModified()`), export-on-save, stale file cleanup via `DocumentFile.delete()`. |

### Tests

| File | Change |
|------|--------|
| `DeviceIdentityTest.kt` | Removed Syncthing JSON parsing tests, kept fallback ID generation tests. |
| `PerDeviceSyncConfigTest.kt` | Removed filesystem listing tests (methods no longer exist), kept `containerFileName` tests. |
| `KeeShareContainerTest.kt` | Unchanged — already tests stream-based `detectFormat()`. |

---

## Verification

```
assembleLibreDebug     ✓  (0 errors)
testDebugUnitTest      ✓  (44 passed, 0 failed)
grep syncthing app/ database/  →  0 matches
grep HttpURLConnection keeshare/  →  0 matches
grep java.io.File keeshare/  →  only context.cacheDir (binary pool temp, same as SaveDatabaseRunnable)
```

---

## Not Changed

| File | Reason |
|------|--------|
| `KeeShareReference.kt` | Pure XML parsing, no I/O |
| `KeeShareImport.kt` | Lambda signatures already stream-based |
| `DatabaseKDBXMerger.kt` | In-memory merge logic, no I/O |
| `KeeShareContainer.read()`/`writeUnsigned()` | Already stream-based |

---

## SAF Patterns Used

All from existing KeePassDX codebase:

- `DocumentFile.fromTreeUri()` for directory access
- `contentResolver.openInputStream(uri)` / `openOutputStream(uri)` for file I/O
- `DocumentFile.listFiles()`, `.findFile()`, `.createFile()`, `.delete()`, `.lastModified()` for directory operations
