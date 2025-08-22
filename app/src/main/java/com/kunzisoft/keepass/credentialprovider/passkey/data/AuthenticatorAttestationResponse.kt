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

import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Encode
import com.kunzisoft.keepass.utils.UUIDUtils.asBytes
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AuthenticatorAttestationResponse(
    private val requestOptions: PublicKeyCredentialCreationOptions,
    private val credentialId: ByteArray,
    private val credentialPublicKey: ByteArray,
    private val userPresent: Boolean,
    private val userVerified: Boolean,
    private val backupEligibility: Boolean,
    private val backupState: Boolean,
    private val publicKeyTypeId: Long,
    private val publicKeyCbor: ByteArray,
    private val clientDataResponse: ClientDataResponse,
) : AuthenticatorResponse {

    override var clientJson = JSONObject()
    var attestationObject: ByteArray

    init {
        attestationObject = defaultAttestationObject()
    }

    private fun buildAuthData(): ByteArray {
        return AuthenticatorData.buildAuthenticatorData(
            relyingPartyId = requestOptions.relyingPartyEntity.id.toByteArray(),
            userPresent = userPresent,
            userVerified = userVerified,
            backupEligibility = backupEligibility,
            backupState = backupState,
            attestedCredentialData = true
        ) + AAGUID +
            //credIdLen
            byteArrayOf((credentialId.size shr 8).toByte(), credentialId.size.toByte()) +
            credentialId +
            credentialPublicKey
    }

    internal fun defaultAttestationObject(): ByteArray {
        // https://www.w3.org/TR/webauthn-3/#attestation-object
        val ao = mutableMapOf<String, Any>()
        ao.put("fmt", "none")
        ao.put("attStmt", emptyMap<Any, Any>())
        ao.put("authData", buildAuthData())
        return Cbor().encode(ao)
    }

    override fun json(): JSONObject {
        // See AuthenticatorAttestationResponseJSON at
        // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson
        return clientJson.apply {
            put("clientDataJSON", clientDataResponse.buildResponse())
            put("authenticatorData", b64Encode(buildAuthData()))
            put("transports", JSONArray(listOf("internal", "hybrid")))
            put("publicKey", b64Encode(publicKeyCbor))
            put("publicKeyAlgorithm", publicKeyTypeId)
            put("attestationObject", b64Encode(attestationObject))
        }
    }

    companion object {
        // Authenticator Attestation Global Unique Identifier
        private val AAGUID: ByteArray = UUID.fromString("eaecdef2-1c31-5634-8639-f1cbd9c00a08").asBytes()
    }
}