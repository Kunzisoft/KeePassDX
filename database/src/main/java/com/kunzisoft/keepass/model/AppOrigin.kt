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
import android.util.Log
import com.kunzisoft.encrypt.Signature.fingerprintToUrlSafeBase64
import com.kunzisoft.keepass.model.WebOrigin.Companion.WEB_ORIGIN_DEFAULT_SCHEME
import kotlinx.parcelize.Parcelize

/**
 * Represents an Android app origin by a list of [AndroidOrigin] and a list of [WebOrigin].
 * If at least one [AndroidOrigin] is verified, the [verified] flag is set to true.
 */
@Parcelize
data class AppOrigin(
    val verified: Boolean,
    val androidOrigins: MutableList<AndroidOrigin> = mutableListOf(),
    val webOrigins: MutableList<WebOrigin> = mutableListOf(),
) : Parcelable {

    fun addAndroidOrigin(androidOrigin: AndroidOrigin) {
        androidOrigins.add(androidOrigin)
    }

    fun addWebOrigin(webOrigin: WebOrigin) {
        this.webOrigins.add(webOrigin)
    }

    /**
     * Determine whether at least one signature is present in the Android origins
     */
    fun containsAndroidOriginSignature(): Boolean {
        return androidOrigins.any { !it.fingerprint.isNullOrEmpty() }
    }

    /**
     * Verify the app origin by comparing it to the list of android origins,
     * return the first verified origin or throw an exception if none is found
     */
    fun checkAppOrigin(compare: AppOrigin): String {
        if (compare.containsAndroidOriginSignature().not()) {
            throw SignatureNotFoundException(this, "Android origin not found")
        }
        return androidOrigins.firstOrNull { androidOrigin ->
            compare.androidOrigins.any {
                it.packageName == androidOrigin.packageName
                        && it.fingerprint == androidOrigin.fingerprint
            }
        }?.let {
            AndroidOrigin(
                packageName = it.packageName,
                fingerprint = it.fingerprint
            ).toOriginValue()
        } ?: throw SecurityException("Wrong signature for ${toName()}")
    }

    fun clear() {
        androidOrigins.clear()
        webOrigins.clear()
    }

    fun isEmpty(): Boolean {
        return androidOrigins.isEmpty() && webOrigins.isEmpty()
    }

    fun toName(): String? {
        return if (androidOrigins.isNotEmpty()) {
            androidOrigins.first().packageName
        } else if (webOrigins.isNotEmpty()){
            webOrigins.first().origin
        } else null
    }

    override fun toString(): String {
        return if (androidOrigins.isNotEmpty()) {
            androidOrigins.first().toString()
        } else if (webOrigins.isNotEmpty()) {
            webOrigins.first().toString()
        } else super.toString()
    }

    companion object {

        private val TAG = AppOrigin::class.java.simpleName

        fun fromOrigin(origin: String, androidOrigin: AndroidOrigin, verified: Boolean): AppOrigin {
            val appOrigin = AppOrigin(verified)
            if (origin.startsWith(WEB_ORIGIN_DEFAULT_SCHEME)) {
                appOrigin.apply {
                    addWebOrigin(WebOrigin(origin))
                }
            } else {
                Log.w(TAG, "Unknown verified origin $origin")
                appOrigin.apply {
                    addAndroidOrigin(androidOrigin)
                }
            }
            return appOrigin
        }
    }
}

/**
 * Exception indicating that no signature is present for the Android origin
 */
class SignatureNotFoundException(
    val temptingApp: AppOrigin,
    message: String
) : Exception(message)

/**
 * Represents an Android app origin, the [packageName] is the applicationId of the app
 * and the [fingerprint] is the
 */
@Parcelize
data class AndroidOrigin(
    val packageName: String,
    val fingerprint: String?
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
    fun toOriginValue(): String {
        if (fingerprint == null) {
            throw IllegalArgumentException("Fingerprint $fingerprint cannot be null")
        }
        return "android:apk-key-hash:${fingerprintToUrlSafeBase64(fingerprint)}"
    }

    override fun toString(): String {
        return "$packageName (${fingerprint})"
    }
}

@Parcelize
data class WebOrigin(
    val origin: String
) : Parcelable {

    fun toOriginValue(): String {
        return origin
    }

    fun defaultAssetLinks(): String {
        return "${origin}/.well-known/assetlinks.json"
    }

    override fun toString(): String {
        return origin
    }

    companion object {
        const val WEB_ORIGIN_DEFAULT_SCHEME = "https"
        const val WEB_ORIGIN_SCHEME_SEPARATOR = "://"

        fun fromDomain(domain: String, scheme: String? = null): WebOrigin {
            return if (domain.contains(WEB_ORIGIN_SCHEME_SEPARATOR)) {
                WebOrigin(domain)
            } else {
                val webScheme = if (scheme.isNullOrEmpty()) WEB_ORIGIN_DEFAULT_SCHEME else scheme
                WebOrigin("$webScheme$WEB_ORIGIN_SCHEME_SEPARATOR$domain")
            }
        }
    }
}