/*
 * Copyright 2023 The KeepassDX Team
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
package com.kunzisoft.keepass.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.google.GoogleDriveHelper

class SyncPreferenceFragment : PreferenceFragmentCompat() {

    private lateinit var googleDriveHelper: GoogleDriveHelper
    private var googleSignInAccount: GoogleSignInAccount? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_sync, rootKey)

        googleDriveHelper = GoogleDriveHelper(requireContext())

        findPreference<Preference>("google_drive_sign_in")?.setOnPreferenceClickListener {
            val signInIntent = googleDriveHelper.signIn()
            startActivityForResult(signInIntent, RC_SIGN_IN)
            true
        }

        findPreference<Preference>("google_drive_sign_out")?.setOnPreferenceClickListener {
            // TODO: Implement sign out
            true
        }

        findPreference<Preference>("google_drive_backup")?.setOnPreferenceClickListener {
            googleSignInAccount?.let { account ->
                val driveService = googleDriveHelper.getDriveService(account)
                val database = (requireActivity() as SettingsActivity).getDatabase()
                if (database != null) {
                    val file = requireContext().getDatabasePath(database.name)
                    googleDriveHelper.uploadFile(driveService, "root", file.name, file) { fileId ->
                        if (fileId != null) {
                            // TODO: Show success message
                        } else {
                            // TODO: Show error message
                        }
                    }
                }
            }
            true
        }

        findPreference<Preference>("google_drive_restore")?.setOnPreferenceClickListener {
            googleSignInAccount?.let { account ->
                val driveService = googleDriveHelper.getDriveService(account)
                googleDriveHelper.listFiles(driveService, "root") { files ->
                    if (files != null) {
                        val fileNames = files.map { it.name }.toTypedArray()
                        val builder = android.app.AlertDialog.Builder(requireContext())
                        builder.setTitle("Select a backup to restore")
                        builder.setItems(fileNames) { _, which ->
                            val selectedFile = files[which]
                            googleDriveHelper.downloadFile(driveService, selectedFile.id) { outputStream ->
                                if (outputStream != null) {
                                    val database = (requireActivity() as SettingsActivity).getDatabase()
                                    if (database != null) {
                                        val file = requireContext().getDatabasePath(database.name)
                                        val fileOutputStream = java.io.FileOutputStream(file)
                                        outputStream.writeTo(fileOutputStream)
                                        fileOutputStream.close()
                                        // TODO: Show success message and restart app
                                    }
                                } else {
                                    // TODO: Show error message
                                }
                            }
                        }
                        builder.show()
                    } else {
                        // TODO: Show error message
                    }
                }
            }
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                googleDriveHelper.handleSignInResult(data) { account ->
                    googleSignInAccount = account
                    updateUi()
                }
            }
        }
    }

    private fun updateUi() {
        if (googleSignInAccount != null) {
            findPreference<Preference>("google_drive_account")?.summary = googleSignInAccount?.email
            findPreference<Preference>("google_drive_sign_in")?.isEnabled = false
            findPreference<Preference>("google_drive_sign_out")?.isEnabled = true
            findPreference<Preference>("google_drive_backup")?.isEnabled = true
            findPreference<Preference>("google_drive_restore")?.isEnabled = true
        } else {
            findPreference<Preference>("google_drive_account")?.summary = "Not signed in"
            findPreference<Preference>("google_drive_sign_in")?.isEnabled = true
            findPreference<Preference>("google_drive_sign_out")?.isEnabled = false
            findPreference<Preference>("google_drive_backup")?.isEnabled = false
            findPreference<Preference>("google_drive_restore")?.isEnabled = false
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
