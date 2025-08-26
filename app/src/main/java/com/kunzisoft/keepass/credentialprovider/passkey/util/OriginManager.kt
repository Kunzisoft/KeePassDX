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

import android.content.pm.SigningInfo
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.P)
class OriginManager(
    private val providedClientDataHash: ByteArray?,
    private val callingAppInfo: CallingAppInfo?,
    private val assets: AssetManager
) {

    fun getOriginAtCreation(
        onOriginRetrieved: (origin: String, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (origin: String, signingInfo: SigningInfo) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { callOrigin, clientDataHash ->
                onOriginRetrieved(callOrigin, clientDataHash)
            },
            onOriginNotRetrieved = { packageName, signingInfo ->
                // Create a new Android Origin and prepare the signature app storage
                onOriginCreated(buildAndroidOrigin(packageName), signingInfo)
            }
        )
    }

    fun getOriginAtUsage(
        storedPackageName: String?,
        storedSignature: SigningInfo?,
        onOriginRetrieved: (origin: String, clientDataHash: ByteArray) -> Unit,
        onOriginCreated: (origin: String) -> Unit
    ) {
        getOrigin(
            onOriginRetrieved = { callOrigin, clientDataHash ->
                onOriginRetrieved(callOrigin, clientDataHash)
            },
            onOriginNotRetrieved = { packageName, signingInfo ->
                // Verify the app signature to retrieve the origin
                // TODO if (packageName == storedPackageName
                //    && signingInfo == storedSignature) {
                    onOriginCreated(buildAndroidOrigin(packageName))
                //} else {
                //    throw SecurityException("Android Origin cannot be retrieved, wrong signature")
                //}
            }
        )
    }

    private fun getOrigin(
        onOriginRetrieved: (callOrigin: String, clientDataHash: ByteArray) -> Unit,
        onOriginNotRetrieved: (packageName: String, signingInfo: SigningInfo) -> Unit
    ) {
        if (callingAppInfo == null) {
            throw SecurityException("Calling app info cannot be retrieved")
        }
        CoroutineScope(Dispatchers.IO).launch {
            var callOrigin: String?
            val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
                it.readText()
            }
            // for trusted browsers like Chrome and Firefox
            callOrigin = callingAppInfo.getOrigin(privilegedAllowlist)?.removeSuffix("/")
            withContext(Dispatchers.Main) {
                if (callOrigin != null && providedClientDataHash != null) {
                    Log.d(TAG, "Origin $callOrigin retrieved from callingAppInfo")
                    onOriginRetrieved(callOrigin, providedClientDataHash)
                } else {
                    onOriginNotRetrieved(callingAppInfo.packageName, callingAppInfo.signingInfo)
                }
            }
        }
    }

    private fun buildAndroidOrigin(packageName: String): String {
        val packageOrigin = "androidapp://${packageName}"
        Log.d(TAG, "Origin $packageOrigin retrieved from package name")
        return packageOrigin
    }

    companion object {
        private val TAG = OriginManager::class.simpleName
    }
}