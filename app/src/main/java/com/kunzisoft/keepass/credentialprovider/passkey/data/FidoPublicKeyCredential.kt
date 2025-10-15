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

import org.json.JSONObject

class FidoPublicKeyCredential(
    val id: String,
    val response: AuthenticatorResponse,
    val authenticatorAttachment: String
) {

    fun json(): String {
        // see at https://www.w3.org/TR/webauthn-3/#sctn-authenticator-credential-properties-extension
        val discoverableCredential = true
        val rk = JSONObject()
        rk.put("rk", discoverableCredential)
        val credProps = JSONObject()
        credProps.put("credProps", rk)

        // See RegistrationResponseJSON at
        // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson
        val ret = JSONObject()
        ret.put("id", id)
        ret.put("rawId", id)
        ret.put("type", "public-key")
        ret.put("authenticatorAttachment", authenticatorAttachment)
        ret.put("response", response.json())
        ret.put("clientExtensionResults", JSONObject()) // TODO credProps

        return ret.toString()
    }
}