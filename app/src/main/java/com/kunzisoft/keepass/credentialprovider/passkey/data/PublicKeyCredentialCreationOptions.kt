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

import android.util.Log
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper
import org.json.JSONObject

class PublicKeyCredentialCreationOptions(
    requestJson: String,
    var clientDataHash: ByteArray?
) {
    val json: JSONObject = JSONObject(requestJson)

    val relyingPartyEntity: PublicKeyCredentialRpEntity
    val userEntity: PublicKeyCredentialUserEntity
    val challenge: ByteArray
    val pubKeyCredParams: List<PublicKeyCredentialParameters>

    var timeout: Long
    var excludeCredentials: List<PublicKeyCredentialDescriptor>
    var authenticatorSelection: AuthenticatorSelectionCriteria
    var attestation: String

    init {
        val rpJson = json.getJSONObject("rp")
        relyingPartyEntity = PublicKeyCredentialRpEntity(rpJson.getString("name"), rpJson.getString("id"))
        val rpUser = json.getJSONObject("user")
        val userId = Base64Helper.b64Decode(rpUser.getString("id"))
        userEntity =
            PublicKeyCredentialUserEntity(
                rpUser.getString("name"),
                userId,
                rpUser.getString("displayName")
            )
        challenge = Base64Helper.b64Decode(json.getString("challenge"))
        val pubKeyCredParamsJson = json.getJSONArray("pubKeyCredParams")
        val pubKeyCredParamsTmp: MutableList<PublicKeyCredentialParameters> = mutableListOf()
        for (i in 0 until pubKeyCredParamsJson.length()) {
            val e = pubKeyCredParamsJson.getJSONObject(i)
            pubKeyCredParamsTmp.add(
                PublicKeyCredentialParameters(e.getString("type"), e.getLong("alg"))
            )
        }
        pubKeyCredParams = pubKeyCredParamsTmp.toList()

        timeout = json.optLong("timeout", 0)
        // TODO: Fix excludeCredentials and authenticatorSelection
        excludeCredentials = emptyList()
        authenticatorSelection = AuthenticatorSelectionCriteria("platform", "required")
        attestation = json.optString("attestation", "none")

        Log.i(TAG, "challenge $challenge()")
        Log.i(TAG, "rp $relyingPartyEntity")
        Log.i(TAG, "user $userEntity")
        Log.i(TAG, "pubKeyCredParams $pubKeyCredParams")
        Log.i(TAG, "timeout $timeout")
        Log.i(TAG, "excludeCredentials $excludeCredentials")
        Log.i(TAG, "authenticatorSelection $authenticatorSelection")
        Log.i(TAG, "attestation $attestation")
    }

    companion object {
        private val TAG = PublicKeyCredentialCreationOptions::class.simpleName
    }
}
