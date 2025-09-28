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
    protected var mLockDatabase: Boolean = true

    protected var isResultLauncherRegistered: Boolean = false

    protected val mCredentialUiState = MutableStateFlow<UIState>(UIState.Loading)
    val credentialUiState: StateFlow<UIState> = mCredentialUiState

    fun showError(error: Throwable) {
        Log.e(TAG, "Error on credential provider launch", error)
        mCredentialUiState.value = UIState.ShowError(error)
    }

    open fun onResult() {
        isResultLauncherRegistered = false
    }

    fun setResult(intent: Intent) {
        // Remove the launcher register
        onResult()
        mCredentialUiState.value = UIState.SetActivityResult(
            lockDatabase = mLockDatabase,
            resultCode = RESULT_OK,
            data = intent
        )
    }

    fun cancelResult() {
        onResult()
        mCredentialUiState.value = UIState.SetActivityResult(
            lockDatabase = mLockDatabase,
            resultCode = RESULT_CANCELED
        )
    }

    open fun onDatabaseRetrieved(database: ContextualDatabase?) {
        mDatabase = database
    }

    abstract fun manageSelectionResult(activityResult: ActivityResult)

    abstract fun manageRegistrationResult(activityResult: ActivityResult)

    open fun onExceptionOccurred(e: Throwable) {
        showError(e)
    }

    fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        onDatabaseRetrieved(database)
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

    sealed class UIState {
        object Loading : UIState()
        data class LaunchGroupActivityForSelection(
            val database: ContextualDatabase,
            val searchInfo: SearchInfo?,
            val typeMode: TypeMode
        ): UIState()
        data class LaunchGroupActivityForRegistration(
            val database: ContextualDatabase,
            val registerInfo: RegisterInfo?,
            val typeMode: TypeMode
        ): UIState()
        data class LaunchFileDatabaseSelectActivityForSelection(
            val searchInfo: SearchInfo?,
            val typeMode: TypeMode
        ): UIState()
        data class LaunchFileDatabaseSelectActivityForRegistration(
            val registerInfo: RegisterInfo?,
            val typeMode: TypeMode
        ): UIState()
        data class SetActivityResult(
            val lockDatabase: Boolean,
            val resultCode: Int,
            val data: Intent? = null
        ): UIState()
        data class ShowError(
            val error: Throwable
        ): UIState()
    }

    companion object {
        private val TAG = CredentialLauncherViewModel::class.java.name
    }
}