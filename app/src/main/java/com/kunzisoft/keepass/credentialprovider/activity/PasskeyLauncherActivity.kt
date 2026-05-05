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
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
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
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.getUserVerifiedWithAuth
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.credentialprovider.passkey.data.UserVerificationRequirement
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.addAppOrigin
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.addAuthCode
import com.kunzisoft.keepass.credentialprovider.viewmodel.CredentialLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.PasskeyLauncherViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.AppUtil.randomRequestCode
import com.kunzisoft.keepass.view.toastError
import kotlinx.coroutines.launch
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeyLauncherActivity : AuthenticationLauncherActivity() {

    private val passkeyLauncherViewModel: PasskeyLauncherViewModel by viewModels()

    private var mPasskeySelectionActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            passkeyLauncherViewModel.manageSelectionResult(it)
        }

    private var mPasskeyRegistrationActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            passkeyLauncherViewModel.manageRegistrationResult(it)
        }

    override fun applyCustomStyle(): Boolean = false

    override fun finishActivityIfReloadRequested(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Initialize the parameters
            passkeyLauncherViewModel.initialize(userVerified = intent.getUserVerifiedWithAuth())
            // Retrieve the UI
            passkeyLauncherViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is PasskeyLauncherViewModel.UIState.Loading -> {
                        // Nothing to do
                    }
                    is PasskeyLauncherViewModel.UIState.ShowAppPrivilegedDialog -> {
                        showAppPrivilegedDialog(
                            temptingApp = uiState.temptingApp
                        )
                    }
                    is PasskeyLauncherViewModel.UIState.ShowAppSignatureDialog -> {
                        showAppSignatureDialog(
                            temptingApp = uiState.temptingApp,
                            nodeId = uiState.nodeId
                        )
                    }
                    is PasskeyLauncherViewModel.UIState.UpdateEntry -> {
                        updateEntry(uiState.oldEntry, uiState.newEntry)
                    }
                }
            }
        }
        lifecycleScope.launch {
            passkeyLauncherViewModel.credentialUiState.collect { uiState ->
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
                        passkeyLauncherViewModel.cancelResult()
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForSelection -> {
                        GroupActivity.launchForSelection(
                            context = this@PasskeyLauncherActivity,
                            database = uiState.database,
                            typeMode = uiState.typeMode,
                            searchInfo = uiState.searchInfo,
                            activityResultLauncher = mPasskeySelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchGroupActivityForRegistration -> {
                        GroupActivity.launchForRegistration(
                            context = this@PasskeyLauncherActivity,
                            database = uiState.database,
                            typeMode = uiState.typeMode,
                            registerInfo = uiState.registerInfo,
                            activityResultLauncher = mPasskeyRegistrationActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForSelection -> {
                        FileDatabaseSelectActivity.launchForSelection(
                            context = this@PasskeyLauncherActivity,
                            typeMode = uiState.typeMode,
                            searchInfo = uiState.searchInfo,
                            activityResultLauncher = mPasskeySelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.LaunchFileDatabaseSelectActivityForRegistration -> {
                        FileDatabaseSelectActivity.launchForRegistration(
                            context = this@PasskeyLauncherActivity,
                            typeMode = uiState.typeMode,
                            registerInfo = uiState.registerInfo,
                            activityResultLauncher = mPasskeyRegistrationActivityResultLauncher,
                        )
                    }
                }
            }
        }
    }

    override fun setUserVerified() {
        passkeyLauncherViewModel.setUserVerified()
    }

    override fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        passkeyLauncherViewModel.launchActionIfNeeded(
            intent = intent,
            specialMode = specialMode,
            database = database
        )
    }

    override fun cancelResult() {
        passkeyLauncherViewModel.cancelResult()
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

    /**
     * Display a dialog that asks the user to add an app to the list of privileged apps.
     */
    private fun showAppPrivilegedDialog(
        temptingApp: AndroidPrivilegedApp
    ) {
        Log.w(javaClass.simpleName, "No privileged apps file found")
        AlertDialog.Builder(this@PasskeyLauncherActivity).apply {
            setTitle(getString(R.string.passkeys_privileged_apps_ask_title))
            setMessage(StringBuilder()
                .append(
                    getString(
                        R.string.passkeys_privileged_apps_ask_message,
                        temptingApp.toString()
                    )
                )
                .append("\n\n")
                .append(getString(R.string.passkeys_privileged_apps_explanation))
                .toString()
            )
            setPositiveButton(android.R.string.ok) { _, _ ->
                passkeyLauncherViewModel.saveCustomPrivilegedApp(
                    intent = intent,
                    specialMode = mSpecialMode,
                    database = mDatabase,
                    temptingApp = temptingApp
                )
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                passkeyLauncherViewModel.cancelResult()
            }
            setOnCancelListener {
                passkeyLauncherViewModel.cancelResult()
            }
        }.create().show()
    }

    /**
     * Display a dialog that asks the user to add an app signature in an existing passkey
     */
    private fun showAppSignatureDialog(
        temptingApp: AppOrigin,
        nodeId: UUID
    ) {
        AlertDialog.Builder(this@PasskeyLauncherActivity).apply {
            setTitle(getString(R.string.passkeys_missing_signature_app_ask_title))
            setMessage(StringBuilder()
                .append(
                    getString(
                        R.string.passkeys_missing_signature_app_ask_message,
                        temptingApp.toString()
                    )
                )
                .append("\n\n")
                .append(getString(R.string.passkeys_missing_signature_app_ask_explanation))
                .append("\n\n")
                .append(getString(R.string.passkeys_missing_signature_app_ask_question))
                .toString()
            )
            setPositiveButton(android.R.string.ok) { _, _ ->
                passkeyLauncherViewModel.saveAppSignature(
                    database = mDatabase,
                    temptingApp = temptingApp,
                    nodeId = nodeId
                )
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                passkeyLauncherViewModel.cancelResult()
            }
            setOnCancelListener {
                passkeyLauncherViewModel.cancelResult()
            }
        }.create().show()
    }

    companion object {
        private val TAG = PasskeyLauncherActivity::class.java.name

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
            appOrigin: AppOrigin? = null,
            nodeId: UUID? = null,
            userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED,
            userVerifiedWithAuth: Boolean = true
        ): PendingIntent? {
            return PendingIntent.getActivity(
                context,
                randomRequestCode(),
                Intent(context, PasskeyLauncherActivity::class.java).apply {
                    addSpecialMode(specialMode)
                    addTypeMode(TypeMode.PASSKEY)
                    addSearchInfo(searchInfo)
                    addAppOrigin(appOrigin)
                    addNodeId(nodeId)
                    addAuthCode(nodeId)
                    addUserVerification(userVerification, userVerifiedWithAuth)
                },
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
