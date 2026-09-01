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
 *
 * @property verified True if the origin is verified.
 * @property androidOrigins List of associated Android origins.
 * @property webOrigins List of associated web origins.
 */
@Parcelize
data class AppOrigin(
    val verified: Boolean,
    val androidOrigins: MutableList<AndroidOrigin> = mutableListOf(),
    val webOrigins: MutableList<WebOrigin> = mutableListOf(),
) : Parcelable {

    /**
     * Copy constructor for [AppOrigin].
     * @param appOrigin The [AppOrigin] to copy.
     */
    constructor(appOrigin: AppOrigin) : this(
        appOrigin.verified,
        appOrigin.androidOrigins.toMutableList(),
        appOrigin.webOrigins.toMutableList()
    )

    /**
     * Adds an [AndroidOrigin] to the list if it is not already present.
     * @param androidOrigin The [AndroidOrigin] to add.
     */
    fun addAndroidOrigin(androidOrigin: AndroidOrigin) {
        if (androidOrigins.contains(androidOrigin).not())
            this.androidOrigins.add(androidOrigin)
    }

    /**
     * Adds a [WebOrigin] to the list if it is not already present.
     * @param webOrigin The [WebOrigin] to add.
     */
    fun addWebOrigin(webOrigin: WebOrigin) {
        if (webOrigins.contains(webOrigin).not())
            this.webOrigins.add(webOrigin)
    }

    /**
     * Determine whether at least one signature is present in the Android origins.
     * @return True if at least one [AndroidOrigin] has a non-empty fingerprint.
     */
    fun containsAndroidOriginSignature(): Boolean {
        return androidOrigins.any { !it.fingerprint.isNullOrEmpty() }
    }

    /**
     * Finds a matching [AndroidOrigin] in another [AppOrigin].
     * @param other The other [AppOrigin] to compare with.
     * @return The first matching [AndroidOrigin] found, or null if no match is found or other is null.
     */
    private fun androidOriginIn(other: AppOrigin?): AndroidOrigin? {
        if (other == null)
            return null
        androidOrigins.forEach { androidOrigin ->
            if (other.androidOrigins.any {
                    it.packageName == androidOrigin.packageName
                            && it.fingerprint == androidOrigin.fingerprint
                })
                return AndroidOrigin(
                    packageName = androidOrigin.packageName,
                    fingerprint = androidOrigin.fingerprint
                )
        }
        return null
    }

    /**
     * Checks if this [AppOrigin] contains the same Android origin as another [AppOrigin].
     * @param other The other [AppOrigin] to compare with.
     * @return True if they have at least one common [AndroidOrigin].
     */
    fun isTheSameAndroidOriginThan(other: AppOrigin): Boolean {
        return androidOriginIn(other) != null
    }

    /**
     * Checks if this [AppOrigin] contains the same web origin as another [AppOrigin].
     * @param other The other [AppOrigin] to compare with.
     * @return True if they have at least one common [WebOrigin].
     */
    fun isTheSameWebOriginThan(other: AppOrigin): Boolean {
        return this.webOrigins.any { webOrigin ->
            other.webOrigins.any { it.origin == webOrigin.origin }
        }
    }

    /**
     * Checks if this [AppOrigin] matches another [AppOrigin] by comparing their Android or web origins.
     *
     * The matching logic follows these rules:
     * - If either instance has Android origins, they must share at least one common [AndroidOrigin].
     * - If neither instance has Android origins, they must share at least one common [WebOrigin]
     *
     * @param other The other [AppOrigin] to compare with.
     * @return True if the origins match according to the rules, false otherwise.
     */
    fun isTheSameOriginThan(other: AppOrigin?): Boolean {
        if (this.androidOrigins.isNotEmpty() || (other != null && other.androidOrigins.isNotEmpty())) {
            return other != null && this.isTheSameAndroidOriginThan(other)
        }
        if (this.webOrigins.isNotEmpty() || (other != null && other.webOrigins.isNotEmpty()))
            return other != null && this.isTheSameWebOriginThan(other)
        return false
    }

    /**
     * Verify the app origin by comparing it to the list of android origins,
     * return the first verified origin or throw an exception if none is found.
     * @param compare The [AppOrigin] to compare against.
     * @return The origin value of the matching [AndroidOrigin].
     * @throws SignatureNotFoundException If [compare] has no signatures.
     * @throws SecurityException If no match is found.
     */
    fun checkAndroidOrigin(compare: AppOrigin): String {
        if (compare.containsAndroidOriginSignature().not()) {
            throw SignatureNotFoundException(this, "Android origin not found")
        }
        return androidOriginIn(compare)?.toOriginValue()
            ?: throw SecurityException("Wrong signature for ${toName()}")
    }

    /**
     * Clears all origins.
     */
    fun clear() {
        androidOrigins.clear()
        webOrigins.clear()
    }

    /**
     * Checks if the origin lists are empty.
     * @return True if no Android and no web origins are present.
     */
    fun isEmpty(): Boolean {
        return androidOrigins.isEmpty() && webOrigins.isEmpty()
    }

    /**
     * Gets a display name for the origin.
     * @return The first package name or web origin, or null if empty.
     */
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppOrigin

        // Don't check verified here
        if (androidOrigins.toSet() != other.androidOrigins.toSet()) return false
        if (webOrigins.toSet() != other.webOrigins.toSet()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = androidOrigins.toSet().hashCode()
        result = 31 * result + webOrigins.toSet().hashCode()
        return result
    }

    companion object {

        private val TAG = AppOrigin::class.java.simpleName

        /**
         * Creates an [AppOrigin] from a string origin and an [AndroidOrigin].
         * @param origin The string origin (e.g. starting with https for web origin).
         * @param androidOrigin The [AndroidOrigin] to use if it's not a web origin.
         * @param verified The verified status.
         * @return A new [AppOrigin] instance.
         */
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
 * Represents an Android app origin.
 * @property packageName The applicationId of the app.
 * @property fingerprint The SHA-256 hash of the app's signing certificate (colon-separated hex string).
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
     * @return The Android App Origin string.
     * @throws IllegalArgumentException if the fingerprint is null.
     */
    fun toOriginValue(): String {
        if (fingerprint == null) {
            throw IllegalArgumentException("Fingerprint cannot be null")
        }
        return "android:apk-key-hash:${fingerprintToUrlSafeBase64(fingerprint)}"
    }

    override fun toString(): String {
        return "$packageName (${fingerprint})"
    }
}

/**
 * Represents a web origin.
 * @property origin The full origin string (e.g., "https://example.com").
 */
@Parcelize
data class WebOrigin(
    val origin: String
) : Parcelable {

    /**
     * Returns the raw origin string.
     * @return The origin string.
     */
    fun toOriginValue(): String {
        return origin
    }

    /**
     * Returns the default asset links URL for this origin.
     * @return The URL string.
     */
    fun defaultAssetLinks(): String {
        return "${origin}/.well-known/assetlinks.json"
    }

    override fun toString(): String {
        return origin
    }

    companion object {
        /**
         * The default scheme for web origins if none is provided.
         */
        const val WEB_ORIGIN_DEFAULT_SCHEME = "https"

        /**
         * The separator between scheme and domain.
         */
        const val WEB_ORIGIN_SCHEME_SEPARATOR = "://"

        /**
         * Creates a [WebOrigin] from a domain and optional scheme.
         * @param domain The domain string.
         * @param scheme The scheme string (optional, defaults to [WEB_ORIGIN_DEFAULT_SCHEME]).
         * @return A [WebOrigin] instance or null if domain is empty.
         */
        fun fromDomain(domain: String?, scheme: String? = null): WebOrigin? {
            if (domain.isNullOrEmpty())
                return null
            return if (domain.contains(WEB_ORIGIN_SCHEME_SEPARATOR)) {
                WebOrigin(domain)
            } else {
                val webScheme = if (scheme.isNullOrEmpty()) WEB_ORIGIN_DEFAULT_SCHEME else scheme
                WebOrigin("$webScheme$WEB_ORIGIN_SCHEME_SEPARATOR$domain")
            }
        }
    }
}