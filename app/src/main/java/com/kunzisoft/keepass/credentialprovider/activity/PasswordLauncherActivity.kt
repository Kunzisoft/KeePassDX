/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSpecialMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addTypeMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.setActivityResult
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.addUserVerification
import com.kunzisoft.keepass.credentialprovider.passkey.data.UserVerificationRequirement
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.addAuthCode
import com.kunzisoft.keepass.credentialprovider.viewmodel.CredentialLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.PasswordLauncherViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.AppUtil.randomRequestCode
import com.kunzisoft.keepass.view.toastError
import kotlinx.coroutines.launch
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasswordLauncherActivity : AuthenticationLauncherActivity() {

    private val passwordLauncherViewModel: PasswordLauncherViewModel by viewModels()

    private var mPasswordSelectionActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            passwordLauncherViewModel.manageSelectionResult(it)
        }

    private var mPasswordRegistrationActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            passwordLauncherViewModel.manageRegistrationResult(it)
        }

    override fun applyCustomStyle(): Boolean = false

    override fun finishActivityIfReloadRequested(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Initialize the parameters
            passwordLauncherViewModel.initialize()
            // Retrieve the UI
            passwordLauncherViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is PasswordLauncherViewModel.UIState.Loading -> {
                        // Nothing to do
                    }
                    is PasswordLauncherViewModel.UIState.UpdateEntry -> {
                        updateEntry(uiState.oldEntry, uiState.newEntry)
                    }
                }
            }
        }
        lifecycleScope.launch {
            passwordLauncherViewModel.credentialUiState.collect { uiState ->
                when (uiState) {
                    is CredentialLauncherViewModel.CredentialState.Loading -> {}
                    is CredentialLauncherViewModel.CredentialState.SetActivityResult -> {
                        setActivityResult(
                            lockDatabase = uiState.lockDatabase,
                            resultCode = uiState.resultCode,
                            data = uiState.data
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.ShowError -> {
                        toastError(uiState.error)
                        passwordLauncherViewModel.cancelResult()
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForSelection -> {
                        GroupActivity.launchForSelection(
                            context = this@PasswordLauncherActivity,
                            database = uiState.database,
                            typeMode = uiState.typeMode,
                            searchInfo = uiState.searchInfo,
                            activityResultLauncher = mPasswordSelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForRegistration -> {
                        GroupActivity.launchForRegistration(
                            context = this@PasswordLauncherActivity,
                            database = uiState.database,
                            typeMode = uiState.typeMode,
                            registerInfo = uiState.registerInfo,
                            activityResultLauncher = mPasswordRegistrationActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForSelection -> {
                        FileDatabaseSelectActivity.launchForSelection(
                            context = this@PasswordLauncherActivity,
                            typeMode = uiState.typeMode,
                            searchInfo = uiState.searchInfo,
                            activityResultLauncher = mPasswordSelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForRegistration -> {
                        FileDatabaseSelectActivity.launchForRegistration(
                            context = this@PasswordLauncherActivity,
                            typeMode = uiState.typeMode,
                            registerInfo = uiState.registerInfo,
                            activityResultLauncher = mPasswordRegistrationActivityResultLauncher,
                        )
                    }
                }
            }
        }
    }

    override fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        passwordLauncherViewModel.launchActionIfNeeded(
            intent = intent,
            specialMode = mSpecialMode,
            database = database
        )
    }

    override fun cancelResult() {
        passwordLauncherViewModel.cancelResult()
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                // TODO When auto save is enabled, WARNING filter by the calling activity
                // passkeyLauncherViewModel.autoSelectPasskey(result, database)
            }
        }
    }

    companion object {
        private val TAG = PasswordLauncherActivity::class.java.name

        /**
         * Get a pending intent to launch the passkey launcher activity
         * [nodeId] can be :
         *  - null if manual selection is requested
         *  - null if manual registration is requested
         *  - an entry node id if direct selection is requested
         *  - a group node id if direct registration is requested in a default group
         *  - an entry node id if overwriting is requested in an existing entry
         */
        fun getPendingIntent(
            context: Context,
            specialMode: SpecialMode,
            searchInfo: SearchInfo? = null,
            nodeId: UUID? = null,
            userVerifiedWithAuth: Boolean = true
        ): PendingIntent? {
            return PendingIntent.getActivity(
                context,
                randomRequestCode(),
                Intent(context, PasswordLauncherActivity::class.java).apply {
                    addSpecialMode(specialMode)
                    addTypeMode(TypeMode.PASSWORD)
                    addSearchInfo(searchInfo)
                    addNodeId(nodeId)
                    addAuthCode(nodeId)
                    // User Verification request is always preferred for password,
                    // Allows to configure whether it is necessary or not with the corresponding setting
                    addUserVerification(UserVerificationRequirement.PREFERRED, userVerifiedWithAuth)
                },
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
