package com.kunzisoft.keepass.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class AdvancedUnlockViewModel : ViewModel() {

    var allowAutoOpenBiometricPrompt : Boolean = true
    var deviceCredentialAuthSucceeded: Boolean? = null

    val onInitAdvancedUnlockModeRequested : LiveData<Void?> get() = _onInitAdvancedUnlockModeRequested
    private val _onInitAdvancedUnlockModeRequested = SingleLiveEvent<Void?>()

    val onUnlockAvailabilityCheckRequested : LiveData<Void?> get() = _onUnlockAvailabilityCheckRequested
    private val _onUnlockAvailabilityCheckRequested = SingleLiveEvent<Void?>()

    val onDatabaseFileLoaded : LiveData<Uri?> get() = _onDatabaseFileLoaded
    private val _onDatabaseFileLoaded = SingleLiveEvent<Uri?>()

    fun initAdvancedUnlockMode() {
        _onInitAdvancedUnlockModeRequested.call()
    }

    fun checkUnlockAvailability() {
        _onUnlockAvailabilityCheckRequested.call()
    }

    fun databaseFileLoaded(databaseUri: Uri?) {
        _onDatabaseFileLoaded.value = databaseUri
    }
}