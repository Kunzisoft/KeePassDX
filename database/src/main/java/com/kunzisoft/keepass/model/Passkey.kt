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
data class Passkey(
    val username: String,
    val privateKeyPem: String,
    val credentialId: String,
    val userHandle: String,
    val relyingParty: String,
    val backupEligibility: Boolean?,
    val backupState: Boolean?
): Parcelable {
    // Do not compare BE and BS because are modifiable by the user
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Passkey

        if (username != other.username) return false
        if (privateKeyPem != other.privateKeyPem) return false
        if (credentialId != other.credentialId) return false
        if (userHandle != other.userHandle) return false
        if (relyingParty != other.relyingParty) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + privateKeyPem.hashCode()
        result = 31 * result + credentialId.hashCode()
        result = 31 * result + userHandle.hashCode()
        result = 31 * result + relyingParty.hashCode()
        return result
    }
}
