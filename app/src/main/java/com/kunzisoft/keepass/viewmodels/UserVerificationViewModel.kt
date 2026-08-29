/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.database.element.MasterCredential.CREATOR.getCheckKey
import com.kunzisoft.keepass.database.exception.InvalidCredentialsDatabaseException
import com.kunzisoft.keepass.utils.clear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the User Verification.
 */
class UserVerificationViewModel: ViewModel() {

    private val _onUserVerificationSucceeded = MutableSharedFlow<UserVerificationData>(replay = 0)
    val onUserVerificationSucceeded: SharedFlow<UserVerificationData> = _onUserVerificationSucceeded.asSharedFlow()

    private val _onUserVerificationCanceled = MutableSharedFlow<VerificationCanceled>(replay = 0)
    val onUserVerificationCanceled: SharedFlow<VerificationCanceled> = _onUserVerificationCanceled.asSharedFlow()

    var dataToVerify: UserVerificationData? = null

    /**
     * Check the main credential for user verification.
     * @param checkCharArray The password to check.
     */
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

    /**
     * Notify that user verification succeeded.
     * @param dataToVerify The verification data.
     */
    fun onUserVerificationSucceeded(dataToVerify: UserVerificationData) {
        viewModelScope.launch {
            _onUserVerificationSucceeded.emit(dataToVerify)
        }
    }

    /**
     * Notify that user verification failed or was canceled.
     * @param dataToVerify The verification data.
     * @param error The error that caused the failure.
     */
    fun onUserVerificationFailed(
        dataToVerify: UserVerificationData? = null,
        error: Throwable? = null
    ) {
        this.dataToVerify = dataToVerify
        viewModelScope.launch {
            _onUserVerificationCanceled.emit(VerificationCanceled(dataToVerify, error))
        }
    }

    /**
     * Data class for verification cancellation.
     */
    data class VerificationCanceled(
        val dataToVerify: UserVerificationData?,
        val error: Throwable?
    )
}