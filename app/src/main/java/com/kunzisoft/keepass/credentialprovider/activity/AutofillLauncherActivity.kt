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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.FileDatabaseSelectActivity
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addRegisterInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSpecialMode
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.setActivityResult
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillComponent
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillHelper.addAutofillComponent
import com.kunzisoft.keepass.credentialprovider.viewmodel.AutofillLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.CredentialLauncherViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.utils.AppUtil.randomRequestCode
import com.kunzisoft.keepass.view.toastError
import kotlinx.coroutines.launch

@RequiresApi(api = Build.VERSION_CODES.O)
class AutofillLauncherActivity : DatabaseModeActivity() {

    private val autofillLauncherViewModel: AutofillLauncherViewModel by viewModels()

    private var mAutofillSelectionActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            autofillLauncherViewModel.manageSelectionResult(it)
        }

    private var mAutofillRegistrationActivityResultLauncher: ActivityResultLauncher<Intent>? =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            autofillLauncherViewModel.manageRegistrationResult(it)
        }

    override fun applyCustomStyle(): Boolean {
        return false
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            // Retrieve the UI
            autofillLauncherViewModel.credentialUiState.collect { uiState ->
                when (uiState) {
                    is CredentialLauncherViewModel.UIState.Loading -> {}
                    is CredentialLauncherViewModel.UIState.LaunchGroupActivityForSelection -> {
                        GroupActivity.launchForSelection(
                            context = this@AutofillLauncherActivity,
                            database = uiState.database,
                            searchInfo = uiState.searchInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mAutofillSelectionActivityResultLauncher,
                        )
                    }
                    is CredentialLauncherViewModel.UIState.LaunchGroupActivityForRegistration -> {
                        GroupActivity.launchForRegistration(
                            context = this@AutofillLauncherActivity,
                            database = uiState.database,
                            registerInfo = uiState.registerInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mAutofillRegistrationActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.UIState.LaunchFileDatabaseSelectActivityForSelection -> {
                        FileDatabaseSelectActivity.launchForSelection(
                            context = this@AutofillLauncherActivity,
                            searchInfo = uiState.searchInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mAutofillSelectionActivityResultLauncher
                        )
                    }
                    is CredentialLauncherViewModel.UIState.LaunchFileDatabaseSelectActivityForRegistration -> {
                        FileDatabaseSelectActivity.launchForRegistration(
                            context = this@AutofillLauncherActivity,
                            registerInfo = uiState.registerInfo,
                            typeMode = uiState.typeMode,
                            activityResultLauncher = mAutofillRegistrationActivityResultLauncher,
                        )
                    }
                    is CredentialLauncherViewModel.UIState.SetActivityResult -> {
                        setActivityResult(
                            typeMode = TypeMode.AUTOFILL,
                            lockDatabase = uiState.lockDatabase,
                            resultCode = uiState.resultCode,
                            data = uiState.data
                        )
                    }
                    is CredentialLauncherViewModel.UIState.ShowError -> {
                        toastError(uiState.error)
                        autofillLauncherViewModel.cancelResult()
                    }
                }
            }
        }
        lifecycleScope.launch {
            // Initialize the parameters
            autofillLauncherViewModel.uiState.collect { uiState ->
                when (uiState) {
                    AutofillLauncherViewModel.UIState.Loading -> {}
                    is AutofillLauncherViewModel.UIState.ShowBlockRestartMessage -> {
                        showBlockRestartMessage()
                        autofillLauncherViewModel.cancelResult()
                    }
                    is AutofillLauncherViewModel.UIState.ShowReadOnlyMessage -> {
                        showReadOnlySaveMessage()
                        autofillLauncherViewModel.cancelResult()
                    }
                    is AutofillLauncherViewModel.UIState.ShowAutofillSuggestionMessage -> {
                        showAutofillSuggestionMessage()
                    }
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        autofillLauncherViewModel.launchActionIfNeeded(intent, mSpecialMode, database)
    }

    private fun showBlockRestartMessage() {
        // If item not allowed, show a toast
        Toast.makeText(
            applicationContext,
            R.string.autofill_block_restart,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showAutofillSuggestionMessage() {
        Toast.makeText(
            applicationContext,
            R.string.autofill_inline_suggestions_keyboard,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showReadOnlySaveMessage() {
        toastError(RegisterInReadOnlyDatabaseException())
    }

    companion object {

        private val TAG = AutofillLauncherActivity::class.java.name

        fun getPendingIntentForSelection(
            context: Context,
            searchInfo: SearchInfo? = null,
            autofillComponent: AutofillComponent
        ): PendingIntent? {
            try {
                return PendingIntent.getActivity(
                    context,
                    randomRequestCode(),
                    // Doesn't work with direct extra Parcelable (don't know why?)
                    // Wrap into a bundle to bypass the problem
                    Intent(context, AutofillLauncherActivity::class.java).apply {
                        addSpecialMode(SpecialMode.SELECTION)
                        addSearchInfo(searchInfo)
                        addAutofillComponent(autofillComponent)
                    },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    } else {
                        PendingIntent.FLAG_CANCEL_CURRENT
                    }
                )
            } catch (e: RuntimeException) {
                Log.e(TAG, "Unable to create pending intent for selection", e)
                return null
            }
        }

        fun getPendingIntentForRegistration(
            context: Context,
            registerInfo: RegisterInfo
        ): PendingIntent? {
            try {
                return PendingIntent.getActivity(
                    context,
                    randomRequestCode(),
                    Intent(context, AutofillLauncherActivity::class.java).apply {
                        addSpecialMode(SpecialMode.REGISTRATION)
                        addRegisterInfo(registerInfo)
                    },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    } else {
                        PendingIntent.FLAG_CANCEL_CURRENT
                    }
                )
            } catch (e: RuntimeException) {
                Log.e(TAG, "Unable to create pending intent for registration", e)
                return null
            }
        }

        fun launchForRegistration(
            context: Context,
            registerInfo: RegisterInfo
        ) {
            val intent = Intent(context, AutofillLauncherActivity::class.java)
            intent.addSpecialMode(SpecialMode.REGISTRATION)
            intent.addRegisterInfo(registerInfo)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
