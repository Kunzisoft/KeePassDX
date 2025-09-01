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
package com.kunzisoft.keepass.model

import android.os.Parcelable
import com.kunzisoft.encrypt.HashManager.fingerprintToUrlSafeBase64
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppOrigin(
    val androidOrigins: MutableList<AndroidOrigin> = mutableListOf(),
    val webOrigins: MutableList<WebOrigin> = mutableListOf()
) : Parcelable {

    fun addAndroidOrigin(androidOrigin: AndroidOrigin) {
        androidOrigins.add(androidOrigin)
    }

    fun addWebOrigin(webOrigin: WebOrigin) {
        this.webOrigins.add(webOrigin)
    }

    val verified: Boolean
        get() = androidOrigins.any { it.verified }

    /**
     * Verify the app origin by comparing it to the list of android origins,
     * return the first verified origin or null if none is found
     */
    fun checkAppOrigin(appToCheck: AppOrigin): String? {
        return androidOrigins.firstOrNull { androidOrigin ->
            appToCheck.androidOrigins.any {
                it.packageName == androidOrigin.packageName
                        && it.fingerprint == androidOrigin.fingerprint
            }
        }?.let {
            AndroidOrigin(it.packageName, it.fingerprint, true)
                .toAndroidOrigin()
        }
    }

    fun clear() {
        androidOrigins.clear()
        webOrigins.clear()
    }

    fun isEmpty(): Boolean {
        return androidOrigins.isEmpty() && webOrigins.isEmpty()
    }

    fun toAppOrigin(): String {
        return androidOrigins.firstOrNull()?.toAndroidOrigin()
            ?: throw SecurityException("No app origin found")
    }

    fun toName(): String? {
        return if (androidOrigins.isNotEmpty()) {
            androidOrigins.first().packageName
        } else if (webOrigins.isNotEmpty()){
            webOrigins.first().origin
        } else null
    }
}

@Parcelize
data class AndroidOrigin(
    val packageName: String,
    val fingerprint: String?,
    val verified: Boolean = true
) : Parcelable {

    /**
     * Creates an Android App Origin string of the form "android:apk-key-hash:<base64_urlsafe_hash>"
     * from a colon-separated hex fingerprint string.
     *
     * The input fingerprint is assumed to be the SHA-256 hash of the app's signing certificate.
     *
     * @param fingerprint The colon-separated hex fingerprint string (e.g., "91:F7:CB:...").
     * @return The Android App Origin string.
     * @throws IllegalArgumentException if the hex string (after removing colons) has an odd length
     *         or contains non-hex characters.
     */
    fun toAndroidOrigin(): String {
        if (fingerprint == null) {
            throw IllegalArgumentException("Fingerprint $fingerprint cannot be null")
        }
        return "android:apk-key-hash:${fingerprintToUrlSafeBase64(fingerprint)}"
    }
}

@Parcelize
data class WebOrigin(
    val origin: String,
    val verified: Boolean = true,
) : Parcelable {

    fun toWebOrigin(): String {
        return origin
    }

    fun defaultAssetLinks(): String {
        return "${origin}/.well-known/assetlinks.json"
    }

    companion object {
        const val RELYING_PARTY_DEFAULT_PROTOCOL = "https"
        fun fromRelyingParty(relyingParty: String, verified: Boolean): WebOrigin = WebOrigin(
            origin ="$RELYING_PARTY_DEFAULT_PROTOCOL://$relyingParty",
            verified = verified
        )
    }
}