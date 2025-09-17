package com.kunzisoft.keepass.credentialprovider.viewmodel

import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.model.AppOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PasskeyLauncherViewModel: ViewModel() {

    var isResultLauncherRegistered: Boolean = false
    val lockDatabase = true

    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState

    fun showAppPrivilegedDialog(temptingApp: AndroidPrivilegedApp) {
        _uiState.value = UIState.ShowAppPrivilegedDialog(temptingApp)
    }

    fun showAppSignatureDialog(temptingApp: AppOrigin) {
        _uiState.value = UIState.ShowAppSignatureDialog(temptingApp)
    }

    sealed class UIState {
        object Loading : UIState()
        data class ShowAppPrivilegedDialog(
            val temptingApp: AndroidPrivilegedApp
        ): UIState()
        data class ShowAppSignatureDialog(
            val temptingApp: AppOrigin
        ): UIState()
    }
}