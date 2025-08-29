/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import com.kunzisoft.encrypt.HashManager.getApplicationSignatures
import com.kunzisoft.keepass.model.AndroidOrigin
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.Verification
import com.kunzisoft.keepass.model.WebOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to manage the origin of credential provider applications
 */
@RequiresApi(Build.VERSION_CODES.P)
class OriginManager(
    private val providedClientDataHash: ByteArray?,
    private val callingAppInfo: CallingAppInfo?,
    private val assets: AssetManager,
    private val relyingParty: String,
) {

    /**
     * Retrieves the Android origin to be stored in the database,
     * call [onOriginRetrieved] if the origin is already calculated by the system
     * call [onOriginCreated] if the origin was created manually, origin is verified if present in the KeePass database
     */
    suspend fun getOriginAtCreation(
        onOriginRetrieved: (appInfoToStore: AppOrigin, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (appInfoToStore: AppOrigin, origin: String) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { androidOrigin, webOrigin, callOrigin, clientDataHash ->
                onOriginRetrieved(
                    AppOrigin().apply {
                        addAndroidOrigin(androidOrigin)
                        addWebOrigin(webOrigin)
                    },
                    clientDataHash
                )
            },
            onOriginNotRetrieved = { appIdentifier, webOrigin ->
                // Create a new Android Origin and prepare the signature app storage
                onOriginCreated(
                    AppOrigin().apply {
                        addAndroidOrigin(appIdentifier)
                        addWebOrigin(webOrigin)
                    },
                    appIdentifier.toAndroidOrigin()
                )
            }
        )
    }

    /**
     * Retrieves the origin to verify usage,
     * calls [onOriginRetrieved] if the origin is already calculated by the system
     * calls [onOriginCreated] if the origin was created manually, origin is verified if present in the KeePass database
     */
    suspend fun getOriginAtUsage(
        appOrigin: AppOrigin,
        onOriginRetrieved: (androidOrigin: AndroidOrigin, webOrigin: WebOrigin, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (androidOrigin: AndroidOrigin, webOrigin: WebOrigin) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { androidOrigin, webOrigin, origin, clientDataHash ->
                onOriginRetrieved(androidOrigin, webOrigin, clientDataHash)
            },
            onOriginNotRetrieved = { appIdentifierToCheck, webOrigin ->
                // Check the app signature in the appOrigin, webOrigin cannot be checked now
                onOriginCreated(
                    AndroidOrigin(
                        packageName = appIdentifierToCheck.packageName,
                        signature = appIdentifierToCheck.signature,
                        verification =
                            if (appOrigin.containsVerifiedAndroidOrigin(appIdentifierToCheck))
                                Verification.MANUALLY_VERIFIED
                            else
                                Verification.NOT_VERIFIED
                    ),
                    webOrigin
                )
            }
        )
    }

    /**
     * Utility method to retrieve the origin asynchronously,
     * checks for the presence of the application in the privilege list of the trustedPackages.json file,
     * call [onOriginRetrieved] if the origin is already calculated by the system and in the privileged list
     * call [onOriginNotRetrieved] if the origin is not retrieved from the system
     */
    private suspend fun getOrigin(
        onOriginRetrieved: (androidOrigin: AndroidOrigin, webOrigin: WebOrigin, origin: String, clientDataHash: ByteArray) -> Unit,
        onOriginNotRetrieved: (androidOrigin: AndroidOrigin, webOrigin: WebOrigin) -> Unit
    ) {
        if (callingAppInfo == null) {
            throw SecurityException("Calling app info cannot be retrieved")
        }
        withContext(Dispatchers.IO) {
            var callOrigin: String?
            val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
                it.readText()
            }
            // for trusted browsers like Chrome and Firefox
            callOrigin = callingAppInfo.getOrigin(privilegedAllowlist)?.removeSuffix("/")
            val androidOrigin = AndroidOrigin(
                packageName = callingAppInfo.packageName,
                signature = callingAppInfo.signingInfo
                    .getApplicationSignatures(),
                verification = Verification.NOT_VERIFIED
            )
            // Check if the webDomain is validated for the
            withContext(Dispatchers.Main) {
                if (callOrigin != null && providedClientDataHash != null) {
                    Log.d(TAG, "Origin $callOrigin retrieved from callingAppInfo")
                    onOriginRetrieved(
                        AndroidOrigin(
                            packageName = androidOrigin.packageName,
                            signature = androidOrigin.signature,
                            verification = Verification.AUTOMATICALLY_VERIFIED
                        ),
                        WebOrigin.fromRelyingParty(
                            relyingParty = relyingParty,
                            verification = Verification.AUTOMATICALLY_VERIFIED
                        ),
                        callOrigin,
                        providedClientDataHash
                    )
                } else {
                    onOriginNotRetrieved(
                        androidOrigin,
                        WebOrigin.fromRelyingParty(
                            relyingParty = relyingParty,
                            verification = Verification.NOT_VERIFIED
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = OriginManager::class.simpleName
    }
}