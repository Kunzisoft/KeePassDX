package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class CredentialLauncherViewModel(application: Application): AndroidViewModel(application) {

    protected var mDatabase: ContextualDatabase? = null

    protected var isResultLauncherRegistered: Boolean = false
    private var mSelectionResult: ActivityResult? = null

    protected val mCredentialUiState = MutableStateFlow<CredentialState>(CredentialState.Loading)
    val credentialUiState: StateFlow<CredentialState> = mCredentialUiState

    fun showError(error: Throwable) {
        Log.e(TAG, "Error on credential provider launch", error)
        mCredentialUiState.value = CredentialState.ShowError(error)
    }

    open fun onResult() {
        isResultLauncherRegistered = false
        mSelectionResult = null
    }

    fun setResult(intent: Intent, lockDatabase: Boolean = false) {
        // Remove the launcher register
        onResult()
        mCredentialUiState.value = CredentialState.SetActivityResult(
            lockDatabase = lockDatabase,
            resultCode = RESULT_OK,
            data = intent
        )
    }

    fun cancelResult(lockDatabase: Boolean = false) {
        onResult()
        mCredentialUiState.value = CredentialState.SetActivityResult(
            lockDatabase = lockDatabase,
            resultCode = RESULT_CANCELED
        )
    }

    private fun onDatabaseRetrieved(database: ContextualDatabase) {
        mDatabase = database
        mSelectionResult?.let { selectionResult ->
            manageSelectionResult(database, selectionResult)
        }
    }

    fun manageSelectionResult(activityResult: ActivityResult) {
        // Waiting for the database if needed
        when (activityResult.resultCode) {
            RESULT_OK -> {
                mSelectionResult = activityResult
                mDatabase?.let { database ->
                    manageSelectionResult(database, activityResult)
                }
            }
            RESULT_CANCELED -> {
                cancelResult()
            }
        }
    }

    open fun manageSelectionResult(database: ContextualDatabase, activityResult: ActivityResult) {
        mSelectionResult = null
    }

    open fun manageRegistrationResult(activityResult: ActivityResult) {}

    open fun onExceptionOccurred(e: Throwable) {
        showError(e)
    }

    open fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        if (database != null) {
            onDatabaseRetrieved(database)
        }
        if (isResultLauncherRegistered.not()) {
            isResultLauncherRegistered = true
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                onExceptionOccurred(e)
            }) {
                launchAction(intent, specialMode, database)
            }
        }
    }

    /**
     * Launch the main action
     */
    protected abstract suspend fun launchAction(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    )

    sealed class CredentialState {
        object Loading : CredentialState()
        data class LaunchGroupActivityForSelection(
            val database: ContextualDatabase,
            val searchInfo: SearchInfo?,
            val typeMode: TypeMode
        ): CredentialState()
        data class LaunchGroupActivityForRegistration(
            val database: ContextualDatabase,
            val registerInfo: RegisterInfo?,
            val typeMode: TypeMode
        ): CredentialState()
        data class LaunchFileDatabaseSelectActivityForSelection(
            val searchInfo: SearchInfo?,
            val typeMode: TypeMode
        ): CredentialState()
        data class LaunchFileDatabaseSelectActivityForRegistration(
            val registerInfo: RegisterInfo?,
            val typeMode: TypeMode
        ): CredentialState()
        data class SetActivityResult(
            val lockDatabase: Boolean,
            val resultCode: Int,
            val data: Intent? = null
        ): CredentialState()
        data class ShowError(
            val error: Throwable
        ): CredentialState()
    }

    companion object {
        private val TAG = CredentialLauncherViewModel::class.java.name
    }
}