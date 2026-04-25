package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.database.element.MasterCredential.CREATOR.getCheckKey
import com.kunzisoft.keepass.database.exception.InvalidCredentialsDatabaseException
import com.kunzisoft.keepass.utils.clear
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the User Verification
 */
class UserVerificationViewModel: ViewModel() {

    private val mUVState = MutableStateFlow<UVState>(UVState.Loading)
    val userVerificationState: StateFlow<UVState> = mUVState.asStateFlow()

    var dataToVerify: UserVerificationData? = null

    fun checkMainCredential(checkCharArray: CharArray) {
        // Check the password part
        val data = dataToVerify
        val database = data?.database
        if (database?.checkKey(getCheckKey(checkCharArray, database.passwordEncoding)) == true)
            onUserVerificationSucceeded(data)
        else {
            onUserVerificationFailed(dataToVerify, InvalidCredentialsDatabaseException())
        }
        dataToVerify = null
        checkCharArray.clear()
    }

    fun onUserVerificationSucceeded(dataToVerify: UserVerificationData) {
        mUVState.value = UVState.OnUserVerificationSucceeded(dataToVerify)
    }

    fun onUserVerificationFailed(
        dataToVerify: UserVerificationData? = null,
        error: Throwable? = null
    ) {
        this.dataToVerify = dataToVerify
        mUVState.value = UVState.OnUserVerificationCanceled(dataToVerify, error)
    }

    fun onUserVerificationReceived() {
        mUVState.value = UVState.Loading
    }

    sealed class UVState {
        object Loading: UVState()
        data class OnUserVerificationSucceeded(
            val dataToVerify: UserVerificationData
        ): UVState()
        data class OnUserVerificationCanceled(
            val dataToVerify: UserVerificationData?,
            val error: Throwable?
        ): UVState()
    }
}