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

@RequiresApi(Build.VERSION_CODES.P)
class OriginManager(
    callingAppInfo: CallingAppInfo?,
    assets: AssetManager,
    private val relyingParty: String
) {
    private val webOrigin: String?
    private val apkSigningCertificate: ByteArray? = callingAppInfo?.signingInfo?.apkContentsSigners
        ?.getOrNull(0)?.toByteArray()

    init {
        val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
            it.readText()
        }
        // for trusted browsers like Chrome and Firefox
        webOrigin = callingAppInfo?.getOrigin(privilegedAllowlist)?.removeSuffix("/")
    }

    // TODO isPrivileged app
    fun checkPrivilegedApp(
        clientDataHash: ByteArray?
    ) {
        val isPrivilegedApp = webOrigin != null
                && webOrigin == relyingParty && clientDataHash != null
        Log.d(TAG, "isPrivilegedApp = $isPrivilegedApp")
        if (!isPrivilegedApp) {
            AppRelyingPartyRelation.isRelationValid(relyingParty, apkSigningCertificate)
        }
    }

    val origin: String
        get() {
            return webOrigin ?: relyingParty
        }

    companion object {
        private val TAG = OriginManager::class.simpleName
    }
}