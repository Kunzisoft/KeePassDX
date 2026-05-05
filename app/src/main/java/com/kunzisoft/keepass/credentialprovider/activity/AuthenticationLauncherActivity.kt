package com.kunzisoft.keepass.credentialprovider.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.UserVerificationActionType
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.checkUserVerification
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.getUserVerifiedWithAuth
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.retrieveUserVerificationRequirement
import com.kunzisoft.keepass.credentialprovider.passkey.data.UserVerificationRequirement
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.settings.PreferencesUtil.isUserVerificationForcedWhenPreferred
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel
import kotlinx.coroutines.launch

/**
 * Abstract class to easily manage credential provider for authentication launcher,
 * Allow to retrieve User Verification in the ceremony
 */
abstract class AuthenticationLauncherActivity: DatabaseLockActivity() {

    protected val userVerificationViewModel: UserVerificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // To prevent auto finish the activity
        mTimeoutEnable = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                userVerificationViewModel.userVerificationState.collect { uiState ->
                    when (uiState) {
                        is UserVerificationViewModel.UVState.Loading -> {}
                        is UserVerificationViewModel.UVState.OnUserVerificationSucceeded -> {
                            val data = uiState.dataToVerify
                            when (data.actionType) {
                                UserVerificationActionType.LAUNCH_AUTHENTICATION_CEREMONY -> {
                                    setUserVerified()
                                    launchActionIfNeeded(
                                        intent = intent,
                                        specialMode = mSpecialMode,
                                        database = uiState.dataToVerify.database
                                    )
                                }
                                else -> {}
                            }
                            userVerificationViewModel.onUserVerificationReceived()
                        }
                        is UserVerificationViewModel.UVState.OnUserVerificationCanceled -> {
                            toastError(uiState.error)
                            cancelResult()
                            userVerificationViewModel.onUserVerificationReceived()
                        }
                    }
                }
            }
        }
    }

    override fun onUnknownDatabaseRetrieved(database: ContextualDatabase?) {
        super.onUnknownDatabaseRetrieved(database)
        // To manage https://github.com/Kunzisoft/KeePassDX/issues/2283
        val userVerificationForcedWhenPreferred = isUserVerificationForcedWhenPreferred(this)
        val userVerificationRequirement = intent.retrieveUserVerificationRequirement()
        val userVerificationNeeded = (userVerificationRequirement == UserVerificationRequirement.REQUIRED
                || (userVerificationForcedWhenPreferred
                && userVerificationRequirement == UserVerificationRequirement.PREFERRED)
                ) && intent.getUserVerifiedWithAuth().not()
        if (mTypeMode.useUserVerification && userVerificationNeeded) {
            // If user verification is needed, it means that the database is open
            // otherwise, it would be verified with auth
            if (database != null) {
                val dataToVerify = UserVerificationData(
                    actionType = UserVerificationActionType.LAUNCH_AUTHENTICATION_CEREMONY,
                    database = database,
                    originName = intent.retrieveSearchInfo()?.toString()
                )
                if (database.allowUserVerification) {
                    checkUserVerification(
                        userVerificationViewModel = userVerificationViewModel,
                        dataToVerify = dataToVerify
                    )
                } else {
                    userVerificationViewModel.onUserVerificationFailed(
                        dataToVerify = dataToVerify,
                        error = SecurityException(
                            "User Verification is not allowed for this opened database"
                        )
                    )
                }
            }
        } else {
            launchActionIfNeeded(
                intent = intent,
                specialMode = mSpecialMode,
                database = database
            )
        }
    }

    open fun setUserVerified() {}

    /**
     * Must be redefined to call the associated viewModel
     */
    abstract fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    )

    abstract fun cancelResult()

    override fun viewToInvalidateTimeout(): View? = null
}