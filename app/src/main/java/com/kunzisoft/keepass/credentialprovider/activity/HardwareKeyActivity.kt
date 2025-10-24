package com.kunzisoft.keepass.credentialprovider.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseModeActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.setActivityResult
import com.kunzisoft.keepass.credentialprovider.viewmodel.CredentialLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel.Companion.addHardwareKey
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel.Companion.addSeed
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel.Companion.buildHardwareKeyChallenge
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel.Companion.isYubikeyDriverAvailable
import com.kunzisoft.keepass.credentialprovider.viewmodel.HardwareKeyLauncherViewModel.UIState
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.AppUtil.openExternalApp
import com.kunzisoft.keepass.view.toastError
import kotlinx.coroutines.launch

/**
 * Special activity to deal with hardware key drivers,
 * return the response to the database service once finished
 */
class HardwareKeyActivity: DatabaseModeActivity(){

    private val mHardwareKeyLauncherViewModel: HardwareKeyLauncherViewModel by viewModels()

    private var activityResultLauncher: ActivityResultLauncher<Intent> =
        this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mHardwareKeyLauncherViewModel.manageSelectionResult(it)
        }

    override fun applyCustomStyle(): Boolean  = false

    override fun showDatabaseDialog(): Boolean = false

    override fun manageDatabaseInfo(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            mHardwareKeyLauncherViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is UIState.Loading -> {}
                    is UIState.ShowHardwareKeyDriverNeeded -> {
                        showHardwareKeyDriverNeeded(
                            this@HardwareKeyActivity,
                            uiState.hardwareKey
                        ) {
                            mDatabaseViewModel.onChallengeResponded(null)
                            finish()
                        }
                    }
                    is UIState.LaunchChallengeActivityForResponse -> {
                        // Send to the driver
                        activityResultLauncher.launch(
                            buildHardwareKeyChallenge(uiState.challenge)
                        )
                    }
                    is UIState.OnChallengeResponded -> {
                        mDatabaseViewModel.onChallengeResponded(uiState.response)
                    }
                }
            }
        }
        lifecycleScope.launch {
            mHardwareKeyLauncherViewModel.credentialUiState.collect { uiState ->
                when (uiState) {
                    is CredentialLauncherViewModel.CredentialState.SetActivityResult -> {
                        setActivityResult(
                            lockDatabase = uiState.lockDatabase,
                            resultCode = uiState.resultCode,
                            data = uiState.data
                        )
                    }
                    is CredentialLauncherViewModel.CredentialState.ShowError -> {
                        toastError(uiState.error)
                        mHardwareKeyLauncherViewModel.cancelResult()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)
        mHardwareKeyLauncherViewModel.launchActionIfNeeded(intent, mSpecialMode, database)
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        finish()
    }

    private fun showHardwareKeyDriverNeeded(
        context: Context,
        hardwareKey: HardwareKey?,
        onDialogDismissed: DialogInterface.OnDismissListener
    ) {
        val builder = AlertDialog.Builder(context)
        builder
            .setMessage(
                context.getString(R.string.error_driver_required, hardwareKey.toString())
            )
            .setPositiveButton(R.string.download) { _, _ ->
                context.openExternalApp(
                    context.getString(R.string.key_driver_app_id),
                    context.getString(R.string.key_driver_url)
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setOnDismissListener(onDialogDismissed)
        builder.create().show()
    }

    companion object {
        private val TAG = HardwareKeyActivity::class.java.simpleName

        fun launchHardwareKeyActivity(
            context: Context,
            hardwareKey: HardwareKey,
            seed: ByteArray?
        ) {
            context.startActivity(
                Intent(
                    context,
                    HardwareKeyActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                addHardwareKey(hardwareKey)
                addSeed(seed)
            })
        }

        fun isHardwareKeyAvailable(
            context: Context,
            hardwareKey: HardwareKey?
        ): Boolean {
            if (hardwareKey == null)
                return false
            return when (hardwareKey) {
                /*
                HardwareKey.FIDO2_SECRET -> {
                    // TODO FIDO2 under development
                    false
                }
                */
                HardwareKey.CHALLENGE_RESPONSE_YUBIKEY -> {
                    // Check available intent
                    isYubikeyDriverAvailable(context)
                }
            }
        }
    }
}