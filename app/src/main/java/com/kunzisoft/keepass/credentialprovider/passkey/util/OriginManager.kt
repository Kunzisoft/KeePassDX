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
import com.kunzisoft.keepass.model.AppIdentifier
import com.kunzisoft.keepass.model.AppOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.P)
class OriginManager(
    private val providedClientDataHash: ByteArray?,
    private val callingAppInfo: CallingAppInfo?,
    private val assets: AssetManager
) {

    suspend fun getOriginAtCreation(
        onOriginRetrieved: (appInfoToStore: AppOrigin, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (appInfoToStore: AppOrigin, origin: String) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { appIdentifier, callOrigin, clientDataHash ->
                onOriginRetrieved(
                    AppOrigin().apply {
                        // Do not store Web Browser AppId -> addIdentifier(appIdentifier)
                        addWebDomain(callOrigin)
                    },
                    clientDataHash
                )
            },
            onOriginNotRetrieved = { appIdentifier ->
                // Create a new Android Origin and prepare the signature app storage
                onOriginCreated(
                    AppOrigin().apply { addIdentifier(appIdentifier) },
                    appIdentifier.buildAndroidOrigin()
                )
            }
        )
    }

    /**
     * Retrieve the Android origin from an [AppOrigin],
     * call [onOriginRetrieved] if the origin is already calculated by the system
     * call [onOriginCreated] if the origin was created manually, origin is verified if present in the KeePass database
     */
    suspend fun getOriginAtUsage(
        appOrigin: AppOrigin?,
        onOriginRetrieved: (appIdentifier: AppIdentifier, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (appIdentifier: AppIdentifier, origin: String, originVerified: Boolean) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { appIdentifier, origin, clientDataHash ->
                onOriginRetrieved(appIdentifier, clientDataHash)
            },
            onOriginNotRetrieved = { appIdentifierToCheck ->
                // Verify the app signature to retrieve the origin
                val androidOrigin = appIdentifierToCheck.buildAndroidOrigin()
                appIdentifierToCheck.checkInAppOrigin(
                    appOrigin = appOrigin,
                    onOriginChecked = {
                        onOriginCreated(appIdentifierToCheck, androidOrigin, true)
                    },
                    onOriginNotChecked = {
                        onOriginCreated(appIdentifierToCheck, androidOrigin, false)
                    }
                )
            }
        )
    }

    private suspend fun getOrigin(
        onOriginRetrieved: (appInfoRetrieved: AppIdentifier, origin: String, clientDataHash: ByteArray) -> Unit,
        onOriginNotRetrieved: (appInfoRetrieved: AppIdentifier) -> Unit
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
            val appIdentifier = AppIdentifier(
                id = callingAppInfo.packageName,
                signature = callingAppInfo.signingInfo
                    .getApplicationSignatures()
            )
            withContext(Dispatchers.Main) {
                if (callOrigin != null && providedClientDataHash != null) {
                    Log.d(TAG, "Origin $callOrigin retrieved from callingAppInfo")
                        onOriginRetrieved(appIdentifier, callOrigin, providedClientDataHash)
                } else {
                        onOriginNotRetrieved(appIdentifier)
                }
            }
        }
    }

    companion object {
        private val TAG = OriginManager::class.simpleName

        /**
         * Verify that the application signature is contained in the [appOrigin]
         */
        fun AppIdentifier.checkInAppOrigin(
            appOrigin: AppOrigin?,
            onOriginChecked: (origin: String) -> Unit,
            onOriginNotChecked: () -> Unit
        ) {
            // Verify the app signature to retrieve the origin
            val appIdentifierStored = appOrigin?.appIdentifiers?.filter {
                it.id == this.id
            }
            if (appIdentifierStored?.any { it.signature == this.signature } == true) {
                onOriginChecked(this.buildAndroidOrigin())
            } else {
                onOriginNotChecked()
            }
        }

        /**
         * Builds an Android Origin from a AppIdentifier
         */
        fun AppIdentifier.buildAndroidOrigin(): String {
            return buildAndroidOrigin(this.id)
        }

        /**
         * Builds an Android Origin from a package name.
         */
        private fun buildAndroidOrigin(packageName: String?): String {
            if (packageName.isNullOrEmpty())
                throw SecurityException("Package name cannot be empty")
            val packageOrigin = "androidapp://${packageName}"
            Log.d(TAG, "Origin $packageOrigin retrieved from package name")
            return packageOrigin
        }
    }
}