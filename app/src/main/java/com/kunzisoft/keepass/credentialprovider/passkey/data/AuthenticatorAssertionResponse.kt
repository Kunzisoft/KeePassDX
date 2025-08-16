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

import androidx.credentials.exceptions.GetCredentialUnknownException
import com.kunzisoft.asymmetric.Signature
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Encode
import org.json.JSONObject

class AuthenticatorAssertionResponse(
    private val requestOptions: PublicKeyCredentialRequestOptions,
    private val userPresent: Boolean,
    private val userVerified: Boolean,
    private val backupEligibility: Boolean,
    private val backupState: Boolean,
    private var userHandle: String,
    privateKey: String,
    private val clientDataResponse: ClientDataResponse,
) : AuthenticatorResponse {

    override var clientJson = JSONObject()
    private var authenticatorData: ByteArray = AuthenticatorData.buildAuthenticatorData(
        relyingPartyId = requestOptions.rpId.toByteArray(),
        userPresent = userPresent,
        userVerified = userVerified,
        backupEligibility = backupEligibility,
        backupState = backupState
    )
    private var signature: ByteArray = byteArrayOf()

    init {
        signature = Signature.sign(privateKey, dataToSign())
            ?: throw GetCredentialUnknownException("signing failed")
    }

    private fun dataToSign(): ByteArray {
        return authenticatorData + clientDataResponse.hashData()
    }

    override fun json(): JSONObject {
        // https://www.w3.org/TR/webauthn-3/#authdata-flags
        return clientJson.apply {
            put("clientDataJSON", clientDataResponse.buildResponse())
            put("authenticatorData", b64Encode(authenticatorData))
            put("signature", b64Encode(signature))
            put("userHandle", userHandle)
        }
    }
}
