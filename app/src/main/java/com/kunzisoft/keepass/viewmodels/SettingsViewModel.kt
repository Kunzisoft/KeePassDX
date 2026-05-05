package com.kunzisoft.keepass.viewmodels

import android.app.Application
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application): AndroidViewModel(application) {

    private val mSettingsState = MutableStateFlow<SettingsState>(SettingsState.Wait)
    val settingsState: StateFlow<SettingsState> = mSettingsState.asStateFlow()


    var dialogFragment: DialogFragment? = null

    fun showError(error: Throwable?) {
        mSettingsState.value = SettingsState.ShowError(error)
    }

    fun errorShown() {
        mSettingsState.value = SettingsState.Wait
    }

    sealed class SettingsState {
        object Wait: SettingsState()
        data class ShowError(
            val error: Throwable? = null
        ): SettingsState()
    }
}