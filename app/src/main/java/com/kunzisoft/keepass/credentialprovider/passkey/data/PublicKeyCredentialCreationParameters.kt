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
package com.kunzisoft.keepass.credentialprovider.passkey.data

import java.security.KeyPair

data class PublicKeyCredentialCreationParameters(
        val publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions,
        val credentialId: ByteArray,
        val signatureKey: Pair<KeyPair, Long>,
        val clientDataResponse: ClientDataResponse
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyCredentialCreationParameters

        if (publicKeyCredentialCreationOptions != other.publicKeyCredentialCreationOptions) return false
        if (!credentialId.contentEquals(other.credentialId)) return false
        if (signatureKey != other.signatureKey) return false
        if (clientDataResponse != other.clientDataResponse) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKeyCredentialCreationOptions.hashCode()
        result = 31 * result + credentialId.contentHashCode()
        result = 31 * result + signatureKey.hashCode()
        result = 31 * result + clientDataResponse.hashCode()
        return result
    }
}