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
package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.util.Base64

class Base64Helper {

    companion object {

        fun b64Decode(encodedString: String): ByteArray {
            return Base64.decode(
                encodedString,
                Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            )
        }

        fun b64Encode(data: ByteArray): String {
            return Base64.encodeToString(
                data,
                Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            )
        }
    }
}