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

    fun containsVerifiedAndroidOrigin(androidOrigin: AndroidOrigin): Boolean {
        return androidOrigins.any {
            it.packageName == androidOrigin.packageName
                    && it.signature == androidOrigin.signature
                    && it.verification.verified
        }
    }

    fun getFirstAndroidOrigin(): AndroidOrigin? {
        return androidOrigins.firstOrNull()
    }

    fun containsVerifiedWebOrigin(webOrigin: WebOrigin): Boolean {
        return this.webOrigins.any {
            it.origin == webOrigin.origin
                    && it.verification.verified
        }
    }

    fun containsUnverifiedWebOrigin(): Boolean {
        return this.webOrigins.any {
            it.verification.verified.not()
        }
    }

    fun firstVerifiedWebOrigin(): WebOrigin? {
        return webOrigins.first {
            it.verification.verified
        }
    }

    fun getFirstWebOrigin(): WebOrigin? {
        return webOrigins.firstOrNull()
    }

    fun firstUnverifiedOrigin(): WebOrigin? {
        return webOrigins.first {
            it.verification.verified.not()
        }
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
}

enum class Verification {
    MANUALLY_VERIFIED, AUTOMATICALLY_VERIFIED, NOT_VERIFIED;

    val verified: Boolean
        get() = this == MANUALLY_VERIFIED || this == AUTOMATICALLY_VERIFIED
}

@Parcelize
data class AndroidOrigin(
    val packageName: String,
    val signature: String? = null,
    val verification: Verification = Verification.AUTOMATICALLY_VERIFIED,
) : Parcelable {

    fun toAndroidOrigin(): String {
        return "android:apk-key-hash:${packageName}"
    }
}

@Parcelize
data class WebOrigin(
    val origin: String,
    val verification: Verification = Verification.AUTOMATICALLY_VERIFIED,
) : Parcelable {

    fun toWebOrigin(): String {
        return origin
    }

    fun defaultAssetLinks(): String {
        return "${origin}/.well-known/assetlinks.json"
    }

    companion object {
        const val RELYING_PARTY_DEFAULT_PROTOCOL = "https"
        fun fromRelyingParty(relyingParty: String, verification: Verification): WebOrigin = WebOrigin(
            origin ="$RELYING_PARTY_DEFAULT_PROTOCOL://$relyingParty",
            verification = verification
        )
    }
}