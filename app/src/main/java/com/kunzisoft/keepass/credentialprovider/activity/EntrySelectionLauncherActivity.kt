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
package com.kunzisoft.keepass.credentialprovider.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.setActivityResult
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.credentialprovider.viewmodel.CredentialLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.EntrySelectionViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.view.toastError
import kotlinx.coroutines.launch

/**
 * Activity to search or select entry in database,
 * Commonly used with Magikeyboard
 */
class EntrySelectionLauncherActivity : DatabaseModeActivity() {

    private val entrySelectionViewModel: EntrySelectionViewModel by viewModels()

    private var mEntrySelectionActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            entrySelectionViewModel.manageSelectionResult(it)
        }

    override fun applyCustomStyle() = false

    override fun finishActivityIfReloadRequested() = false

    override fun manageDatabaseInfo(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entrySelectionViewModel.initialize()
        lifecycleScope.launch {
            // Initialize the parameters
            entrySelectionViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is EntrySelectionViewModel.UIState.Loading -> {}
                    is EntrySelectionViewModel.UIState.PopulateKeyboard -> {
                        MagikeyboardService.addEntryAndLaunchNotificationIfAllowed(
                            context = this@EntrySelectionLauncherActivity,
                            entry = uiState.entryInfo,
                            toast = true
                        )
                    }
                    is EntrySelectionViewModel.UIState.LaunchFileDatabaseSelectForSearch -> {
                        FileDatabaseSelectActivity.launchForSearch(
                            context = this@EntrySelectionLauncherActivity,
                            searchInfo = uiState.searchInfo
                        )
                        finish()
                    }
                    is EntrySelectionViewModel.UIState.LaunchGroupActivityForSearch -> {
                        GroupActivity.launchForSearch(
                            context = this@EntrySelectionLauncherActivity,
                            database = uiState.database,
                            searchInfo = uiState.searchInfo
                        )
                        finish()
                    }
                }
            }
        }
        lifecycleScope.launch {
            // Retrieve the UI
            entrySelectionViewModel.credentialUiState.collect { uiState ->
                when (uiState) {
                    is CredentialLauncherViewModel.CredentialState.Loading -> {}
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForSelection -> {
                        GroupActivity.launchForSelection(
                            context = this@EntrySelectionLauncherActivity,
                            database = uiState.database,
                            searchInfo = uiState.searchInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mEntrySelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForRegistration -> {
                        GroupActivity.launchForRegistration(
                            context = this@EntrySelectionLauncherActivity,
                            database = uiState.database,
                            registerInfo = uiState.registerInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = null // Null to not get any callback
                        )
                        finish()
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForSelection -> {
                        FileDatabaseSelectActivity.launchForSelection(
                            context = this@EntrySelectionLauncherActivity,
                            searchInfo = uiState.searchInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mEntrySelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForRegistration -> {
                        FileDatabaseSelectActivity.launchForRegistration(
                            context = this@EntrySelectionLauncherActivity,
                            registerInfo = uiState.registerInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = null // Null to not get any callback
                        )
                        finish()
                    }
                    is CredentialLauncherViewModel.CredentialState.SetActivityResult -> {
                        setActivityResult(
                            lockDatabase = uiState.lockDatabase,
                            resultCode = uiState.resultCode,
                            data = uiState.data
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.ShowError -> {
                        toastError(uiState.error)
                        entrySelectionViewModel.cancelResult()
                    }
                }
            }
        }
    }

    override fun onUnknownDatabaseRetrieved(database: ContextualDatabase?) {
        super.onUnknownDatabaseRetrieved(database)
        entrySelectionViewModel.launchActionIfNeeded(intent, mSpecialMode, database)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {

        fun launch(
            context: Context,
            searchInfo: SearchInfo? = null
        ) {
            context.startActivity(Intent(
                context,
                EntrySelectionLauncherActivity::class.java
            ).apply {
                addSearchInfo(searchInfo)
                // New task needed because don't launch from an Activity context
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}
