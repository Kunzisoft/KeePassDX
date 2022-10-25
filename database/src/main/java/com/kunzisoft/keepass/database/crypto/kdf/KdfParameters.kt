/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.crypto.kdf

import com.kunzisoft.keepass.utils.bytes16ToUuid
import com.kunzisoft.keepass.utils.uuidTo16Bytes
import com.kunzisoft.keepass.database.crypto.VariantDictionary
import java.io.IOException
import java.util.*

class KdfParameters: VariantDictionary {

    val uuid: UUID

    constructor(uuid: UUID): super() {
        this.uuid = uuid
    }

    constructor(uuid: UUID, d: VariantDictionary): super(d) {
        this.uuid = uuid
    }

    fun setParamUUID() {
        setByteArray(PARAM_UUID, uuidTo16Bytes(uuid))
    }

    companion object {

        private const val PARAM_UUID = "\$UUID"

        @Throws(IOException::class)
        fun deserialize(data: ByteArray): KdfParameters? {
            val dictionary = VariantDictionary.deserialize(data)

            val uuidBytes = dictionary.getByteArray(PARAM_UUID) ?: return null
            val uuid = bytes16ToUuid(uuidBytes)

            return KdfParameters(uuid, dictionary)
        }

        @Throws(IOException::class)
        fun serialize(kdfParameters: KdfParameters): ByteArray {
            return VariantDictionary.serialize(kdfParameters)
        }
    }

}
