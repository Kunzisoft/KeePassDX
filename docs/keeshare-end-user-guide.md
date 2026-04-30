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

## Setup Order

KeeShare configuration is stored inside the database file as CustomData.
**KeePassXC must be configured first** — it writes the sharing settings into
the database. You then copy that configured database to your phone and set up
KeePassDX to use it.

```
1. KeePassXC: configure shared group(s) + save database
2. Copy the .kdbx database file to your phone
3. KeePassDX: open database, configure sync folder, run first sync
```

Do not try to configure KeePassDX first — without the KeeShare CustomData
written by KeePassXC, there is nothing to sync.

---

## Step 1: KeePassXC (Desktop)

### Create a sync folder

Pick a folder that your sync tool will keep in sync between devices.
Example: `~/Sync/KeeShare/`

### Configure a shared group

1. Open your database in KeePassXC
2. Right-click a group and select **Sharing settings**
3. Set:
   - **Type**: Synchronize (bidirectional)
   - **Path**: Point to a subfolder inside your sync folder, e.g.
     `~/Sync/KeeShare/Passwords/`
   - **Password**: Strong password for the container (both devices need this)
4. Click OK, then save the database (Ctrl+S)

KeePassXC immediately exports the group's entries to a container file in the
sync folder, named after its device ID (e.g., `LAPTOP7.kdbx`).

> **Root group sharing**: You *can* configure sharing on the root group to sync
> the entire database, but this is not recommended. Container files include all
> entries in the shared group and its subgroups — sharing root means every entry
> goes into the container. For most users, sharing one or two specific groups
> keeps containers small and gives more control over what syncs where.

### What KeePassXC does on each save

- Exports the shared group to `{DEVICE_ID}.kdbx` in the sync folder
- Imports from all other `.kdbx` files in the sync folder (other devices'
  containers)
- Merges imported entries using timestamp-based conflict resolution

---

## Step 2: Copy the Database to Your Phone

Copy or sync your `.kdbx` database file to your phone. KeePassDX needs to open
the **same database** that has the KeeShare configuration set up in KeePassXC.

You can transfer it via:
- Your folder-sync tool (Syncthing, Nextcloud, etc.)
- USB file transfer
- Cloud storage (Google Drive, Dropbox, etc.)

Make sure the sync folder (containing the container `.kdbx` files) is also
accessible on your phone. If using Syncthing, it syncs to a local folder on
the device automatically.

> KeePassDX accesses sync folders through Android's Storage Access Framework
> (SAF) using `content://` URIs — it does not use filesystem paths directly.

---

## Step 3: KeePassDX (Android)

### 1. First-time configuration

Before your first sync, you need to tell KeePassDX where the sync folder is
and (optionally) set a device ID.

1. Open KeePassDX
2. Go to **Settings > KeeShare**
3. Tap **Sync folder** — a system folder picker appears
4. Navigate to the folder your sync tool delivers container files to (e.g.
   the Syncthing folder on your device) and tap **Use this folder**
5. Android will ask to grant KeePassDX access — tap **Allow**
6. **Device ID** (optional): Leave blank to auto-generate, or enter a short
   custom ID like `pixel7` or `tablet`

The sync folder URI and device ID are saved in preferences and persist across
app restarts. You only need to do this once.

> **Tip:** If you skip the sync folder setting and jump straight to syncing,
> KeePassDX will prompt you with the folder picker automatically on your first
> manual sync.

### 2. First sync (manual)

1. Open your database in KeePassDX
2. Tap the **overflow menu** (three dots) in any group view
3. Select **"Sync KeeShare"**

On the first manual sync, KeePassDX will:
- Import entries from all other devices' container files in the sync directory
- Export your entries to your own container file (`{DEVICE_ID}.kdbx`)
- Save the database
- Show a toast: *"KeeShare: imported N entries, exported N entries"*

This first manual sync establishes your device's container file in the sync
folder so other devices can see your entries.

### 3. Auto-sync (ongoing)

Once a manual sync has succeeded, auto-sync takes over. While the database is
open, KeePassDX monitors the sync folder for changes using Android's
**ContentObserver**, which fires within seconds of your sync tool writing
a new container file.

When a new container file is detected:
- KeePassDX **imports only** — it does not export during auto-sync
- This prevents feedback loops between devices

When you save the database (edit an entry, etc.):
- KeePassDX **exports only** — it writes your container file to the sync
  folder without importing

This split (import-on-detect, export-on-save) matches KeePassXC's behavior
and prevents sync storms.

No manual intervention needed for ongoing sync — just keep the database open
and let the sync tool deliver container files.

---

## Day-to-Day Usage

### Adding a password on desktop

1. Add or edit an entry in a shared group in KeePassXC
2. Save (Ctrl+S) — KeePassXC exports to its container
3. Sync tool delivers the container to your phone
4. KeePassDX picks it up via ContentObserver (or manual sync)
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
| Folder picker never appears | Go to Settings > KeeShare > Sync folder and set it manually |
| "Sync folder not accessible" error | SAF permission may have been revoked. Go to Settings > KeeShare > Sync folder and re-select it |
| Imported 0 entries | Check: container files present in sync folder? Password match? Sync tool finished writing? |
| Exported 0 entries after auto-sync | Normal — auto-sync only imports. Export happens when you save the database |
| Device ID changed | Set a manual ID in Settings > KeeShare. Manually delete old container files from sync folder |
| Entries not appearing on desktop | Verify sync tool delivered `PHONE01.kdbx` to desktop. Re-save in KeePassXC to trigger import |
| Entries not appearing on phone | Verify sync tool delivered desktop's container. Use manual "Sync KeeShare" for immediate check |
| Sync keeps triggering repeatedly | Update to latest build — the sync storm fix separates import and export triggers |

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
They remain in the sync folder until manually deleted. You can safely remove
container files for devices that are no longer syncing.

**Does KeeShare work with KDB databases?**
No. KDBX v4 required (uses CustomData for configuration).
