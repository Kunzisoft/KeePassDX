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

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Encode
import org.json.JSONObject

open class ClientDataNotDefinedResponse(
    type: Type,
    challenge: ByteArray,
    origin: String,
    crossOrigin: Boolean? = null,
    topOrigin: String? = null,
    packageName: String?
): AuthenticatorResponse, ClientDataResponse {
    override var clientJson = JSONObject()

    init {
        // https://w3c.github.io/webauthn/#client-data
        clientJson.put("type", type.value)
        clientJson.put("challenge", b64Encode(challenge))
        clientJson.put("origin", origin)
        crossOrigin?.let {
            clientJson.put("crossOrigin", it)
        }
        topOrigin?.let {
            clientJson.put("topOrigin", it)
        }
        packageName?.let {
            clientJson.put("androidPackageName", packageName)
        }
    }

    override fun json(): JSONObject {
        return clientJson
    }

    enum class Type(val value: String) {
        GET("webauthn.get"), CREATE("webauthn.create")
    }

    override fun buildResponse(): String {
        return b64Encode(json().toString().toByteArray())
    }

    override fun hashData(): ByteArray {
        return HashManager.hashSha256(json().toString().toByteArray())
    }
}