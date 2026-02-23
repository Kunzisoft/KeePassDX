# KeeShare End User Guide

How to set up and use KeeShare to sync password groups between KeePassXC
(desktop) and KeePassDX (Android).

---

## What KeeShare Does

KeeShare lets you share specific groups of passwords between devices. Each
shared group is exported to a small encrypted container file. A folder-sync
tool (Syncthing, Nextcloud, etc.) carries these containers between devices.
Each device merges incoming entries into its own database.

Key points:
- Each device keeps its own full database
- Only the shared groups' entries travel between devices
- Each device writes its own container file (`{DEVICE_ID}.kdbx`) — no write
  conflicts between devices
- Encryption password per shared group, separate from database master password

---

## Prerequisites

| What | Why |
|------|-----|
| KeePassXC (desktop) with per-device KeeShare support | Configure shared groups, export containers |
| KeePassDX (Android) with KeeShare support | Import/export containers via SAF |
| Folder-sync tool (Syncthing, Nextcloud, etc.) | Move container files between devices |
| KDBX v4 database | KeeShare uses CustomData (not available in KDB format) |

---

## Setup: KeePassXC (Desktop)

### 1. Create a sync folder

Pick a folder that your sync tool will keep in sync between devices.
Example: `~/Sync/KeeShare/`

### 2. Configure a shared group

1. Open your database in KeePassXC
2. Right-click a group (or the root group to share everything)
3. Select **Sharing settings**
4. Set:
   - **Type**: Synchronize (bidirectional)
   - **Path**: Point to a subfolder inside your sync folder, e.g.
     `~/Sync/KeeShare/Passwords/`
   - **Password**: Strong password for the container (both devices need this)
5. Click OK, then save the database (Ctrl+S)

KeePassXC immediately exports the group's entries to a container file in the
sync folder, named after its device ID (e.g., `LAPTOP7.kdbx`).

### 3. What KeePassXC does on each save

- Exports the shared group to `{DEVICE_ID}.kdbx` in the sync folder
- Imports from all other `.kdbx` files in the sync folder (other devices'
  containers)
- Merges imported entries using timestamp-based conflict resolution

---

## Setup: KeePassDX (Android)

### 1. Get the database onto your phone

Sync or copy your `.kdbx` database file to your phone. KeePassDX needs to open
the same database that has the KeeShare configuration set up in KeePassXC.

### 2. Make sure the sync folder is accessible

The sync folder (containing the container files) must be accessible on your
phone. If using Syncthing, it syncs to a local folder on the device.

KeePassDX accesses sync folders through Android's Storage Access Framework
(SAF) using `content://` URIs — it does not use filesystem paths directly.

### 3. Configure device ID (optional)

1. Open KeePassDX
2. Go to **Settings > KeeShare**
3. **Device ID**: Leave blank to auto-generate, or enter a custom short ID

The auto-generated ID is a random 7-character hex string, persisted in
preferences.

### 4. Sync

1. Open your database in KeePassDX
2. Tap the **overflow menu** (three dots) in any group view
3. Select **"Sync KeeShare"**

KeePassDX will:
- Import entries from all other devices' container files in the sync directory
- Export your entries to your own container file (`{DEVICE_ID}.kdbx`)
- Save the database
- Show a toast: *"KeeShare: imported N entries, exported N entries"*

### 5. Auto-sync

While the database is open, KeePassDX checks for new container files every
15 minutes and syncs automatically. It also exports after every database save.

No manual intervention needed for ongoing sync — just keep the database open
and let the sync tool deliver container files.

---

## Day-to-Day Usage

### Adding a password on desktop

1. Add or edit an entry in a shared group in KeePassXC
2. Save (Ctrl+S) — KeePassXC exports to its container
3. Sync tool delivers the container to your phone
4. KeePassDX picks it up on the next periodic check (or manual sync)
5. Entry appears in the same group on your phone

### Adding a password on phone

1. Add or edit an entry in a shared group in KeePassDX
2. Save — KeePassDX exports to its container (`PHONE01.kdbx`)
3. Sync tool delivers the container to your desktop
4. KeePassXC imports from the container on next save/sync cycle
5. Entry appears in the same group on desktop

### Conflict resolution

If the same entry is edited on two devices between syncs, the newer
modification timestamp wins. The "losing" version is preserved in the entry's
history and can be restored.

---

## How the Sync Folder Looks

```
~/Sync/KeeShare/
├── Passwords/
│   ├── LAPTOP7.kdbx     ← KeePassXC's container
│   ├── PHONE01.kdbx     ← KeePassDX phone's container
│   └── TABLET.kdbx      ← KeePassDX tablet's container
├── Work/
│   ├── LAPTOP7.kdbx
│   └── PHONE01.kdbx
```

Each shared group gets its own subfolder. Each device writes only to its own
file and reads from all others.

---

## Identifying Shared Groups

In KeePassDX, groups with KeeShare configuration display a small share icon
in the group list. This indicates the group participates in sync.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Sync KeeShare" menu missing | Database must be KDBX, non-read-only, in group view |
| Imported 0 entries | Check: container files present in sync folder? Password match? Sync tool finished? |
| Device ID changed | Set a manual ID in Settings > KeeShare. Old container auto-cleaned after 90 days |
| Entries not appearing on desktop | Verify sync tool delivered `PHONE01.kdbx` to desktop. Re-save in KeePassXC to trigger import |
| Entries not appearing on phone | Verify sync tool delivered desktop's container. Use manual "Sync KeeShare" for immediate check |

---

## FAQ

**Do I need Syncthing specifically?**
No. Any folder-sync tool works — Syncthing, Nextcloud, Google Drive, Dropbox,
or even manual USB copy. KeeShare just reads and writes files in a folder.

**Does KeeShare send passwords over the internet?**
KeeShare itself never touches the network. It only reads/writes local files.
Your sync tool determines whether data travels over a network. Container files
are encrypted regardless.

**Can I share only specific groups, not the whole database?**
Yes. Configure KeeShare on individual groups in KeePassXC. Only those groups'
entries are synced.

**What happens to old device containers from retired devices?**
Automatically deleted after 90 days of no modification. Configurable threshold.

**Does KeeShare work with KDB databases?**
No. KDBX v4 required (uses CustomData for configuration).
