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

import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper
import org.json.JSONObject

class PublicKeyCredentialRequestOptions(requestJson: String) {
    val json: JSONObject = JSONObject(requestJson)
    val challenge: ByteArray = Base64Helper.b64Decode(json.getString("challenge"))
    val timeout: Long = json.optLong("timeout", 0)
    val rpId: String = json.optString("rpId", "")
    val userVerification: String = json.optString("userVerification", "preferred")
}