/**
 * Created by J-Jamet on 12/02/2026.
 * Copyright (c) 2026 Jeremy Jamet / Kunzisoft. All rights reserved.
 *
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePassDX. If not, see <https://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.database.element.database

import com.kunzisoft.keepass.database.crypto.VariantDictionary
import java.io.IOException

/**
 * Manage the public data in the header of a KDBX v4 Database
 */
class PublicCustomData: VariantDictionary {

    var fidoCredentials: List<ByteArray>
        get() {
            val value = ArrayList<ByteArray>()
            (0..10)
                .mapNotNull { i -> getByteArray(FIDO_CREDENTIAL_X_KEY + i)
            }
            getByteArray(FIDO_CREDENTIAL_KEY)?.let {
                value.add(it)
            }
            return value
        }
        set(value) {
            value.forEachIndexed { i, bytes ->
                setByteArray(FIDO_CREDENTIAL_X_KEY + (i), bytes)
            }
        }

    // TODO manage one credential
    var fidoCredentialId: ByteArray?
        get() = getByteArray(FIDO_CREDENTIAL_KEY)
            ?: getByteArray(FIDO_CREDENTIAL_X_KEY + 0)
        set(value) {
            value?.let {
                setByteArray(FIDO_CREDENTIAL_X_KEY + 0, value)
            }
        }

    constructor(): super()

    private constructor(d: VariantDictionary): super(d)

    companion object {
        private const val FIDO_CREDENTIAL_KEY = "fido_credential"
        private const val FIDO_CREDENTIAL_X_KEY = FIDO_CREDENTIAL_KEY + "_"

        @Throws(IOException::class)
        fun deserialize(data: ByteArray): PublicCustomData {
            return PublicCustomData(VariantDictionary.deserialize(data))
        }
    }
}