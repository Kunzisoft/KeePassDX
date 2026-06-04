/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.action

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.settings.PreferencesUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseBackupUtil {

    private const val TAG = "DatabaseBackupUtil"
    private const val BACKUP_DIR = "database_backups"
    private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun createBackup(context: Context, fileUri: Uri?) {
        if (fileUri == null) return
        if (!PreferencesUtil.isAutoBackupBeforeSaveEnabled(context)) return
        try {
            val backupDir = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }
            val timestamp = DATE_FORMAT.format(Date())
            val backupFile = File(backupDir, "backup_${timestamp}.kdbx.bak")
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            pruneOldBackups(context, backupDir)
        } catch (e: Exception) {
            Log.w(TAG, "Backup failed, continuing with save", e)
        }
    }

    private fun pruneOldBackups(context: Context, backupDir: File) {
        val maxVersions = PreferencesUtil.getAutoBackupMaxVersions(context)
        val backups = backupDir.listFiles { f -> f.name.endsWith(".kdbx.bak") }
            ?.sortedBy { it.lastModified() }
            ?: return
        if (backups.size > maxVersions) {
            backups.take(backups.size - maxVersions).forEach { it.delete() }
        }
    }
}
