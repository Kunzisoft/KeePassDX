package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.MainCredential
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Main Credential Dialog
 * Easily retrieves main credential from the database identified by its URI
 */
class MainCredentialDialogViewModel: ViewModel() {

    private val mMainCredentialMainCredentialEvent = MutableSharedFlow<MainCredentialEvent>()
    val mainCredentialEvent: SharedFlow<MainCredentialEvent> = mMainCredentialMainCredentialEvent.asSharedFlow()

    fun validateMainCredential(
        databaseUri: Uri,
        mainCredential: MainCredential
    ) {
        viewModelScope.launch {
            mMainCredentialMainCredentialEvent.emit(
                MainCredentialEvent.OnMainCredentialEntered(databaseUri, mainCredential)
            )
        }
    }

    fun cancelMainCredential(
        databaseUri: Uri?,
        error: Throwable? = null
    ) {
        viewModelScope.launch {
            mMainCredentialMainCredentialEvent.emit(
                MainCredentialEvent.OnMainCredentialCanceled(databaseUri, error)
            )
        }
    }

    sealed class MainCredentialEvent {
        data class OnMainCredentialEntered(
            val databaseUri: Uri,
            val mainCredential: MainCredential
        ): MainCredentialEvent()
        data class OnMainCredentialCanceled(
            val databaseUri: Uri?,
            val error: Throwable?
        ): MainCredentialEvent()
    }

}