package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.MainCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Main Credential Dialog
 * Easily retrieves main credential from the database identified by its URI
 */
class MainCredentialViewModel: ViewModel() {

    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState.asStateFlow()

    fun validateMainCredential(
        databaseUri: Uri,
        mainCredential: MainCredential
    ) {
        mUiState.value = UIState.OnMainCredentialEntered(databaseUri, mainCredential)
    }

    fun cancelMainCredential(
        databaseUri: Uri?,
        error: Throwable? = null
    ) {
        mUiState.value = UIState.OnMainCredentialCanceled(databaseUri, error)
    }

    fun onActionReceived() {
        mUiState.value = UIState.Loading
    }

    sealed class UIState {
        object Loading: UIState()
        data class OnMainCredentialEntered(
            val databaseUri: Uri,
            val mainCredential: MainCredential
        ): UIState()
        data class OnMainCredentialCanceled(
            val databaseUri: Uri?,
            val error: Throwable?
        ): UIState()
    }

}