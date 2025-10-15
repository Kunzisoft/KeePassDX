package com.kunzisoft.keepass.settings.preferencedialogfragment.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.deletePrivilegedAppsFile
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.retrieveCustomPrivilegedApps
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.retrievePredefinedPrivilegedApps
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.saveCustomPrivilegedApps
import com.kunzisoft.keepass.utils.AppUtil.getInstalledBrowsersWithSignatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeysPrivilegedAppsViewModel(application: Application): AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun retrievePrivilegedAppsToSelect() {
        viewModelScope.launch {
            val predefinedPrivilegedApps = retrievePredefinedPrivilegedApps(getApplication())
            val customPrivilegedApps = retrieveCustomPrivilegedApps(getApplication())
            // Only retrieve browser apps that are not already in the predefined list
            val browserApps = getInstalledBrowsersWithSignatures(getApplication()).filter {
                predefinedPrivilegedApps.none { privilegedApp ->
                    privilegedApp.packageName == it.packageName
                            && privilegedApp.fingerprints.any {
                            fingerprint -> fingerprint in it.fingerprints
                    }
                }
            }
            _uiState.value = UiState.OnPrivilegedAppsToSelectRetrieved(
                privilegedApps = browserApps,
                selected = customPrivilegedApps
            )
        }
    }

    fun saveSelectedPrivilegedApp(privilegedApps: List<AndroidPrivilegedApp>) {
        viewModelScope.launch {
            if (privilegedApps.isNotEmpty())
                saveCustomPrivilegedApps(getApplication(), privilegedApps)
            else
                deletePrivilegedAppsFile(getApplication())
        }
    }

    sealed class UiState {

        object Loading : UiState()
        data class OnPrivilegedAppsToSelectRetrieved(
            val privilegedApps: List<AndroidPrivilegedApp>,
            val selected: List<AndroidPrivilegedApp>
        ) : UiState()
    }
}