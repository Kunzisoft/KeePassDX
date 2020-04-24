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
package com.kunzisoft.keepass.crypto.keyDerivation

import com.kunzisoft.keepass.stream.LittleEndianDataInputStream
import com.kunzisoft.keepass.stream.LittleEndianDataOutputStream
import com.kunzisoft.keepass.stream.bytes16ToUuid
import com.kunzisoft.keepass.stream.uuidTo16Bytes
import com.kunzisoft.keepass.utils.VariantDictionary
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class KdfParameters(val uuid: UUID) : VariantDictionary() {

    fun setParamUUID() {
        setByteArray(PARAM_UUID, uuidTo16Bytes(uuid))
    }

    companion object {

        private const val PARAM_UUID = "\$UUID"

        @Throws(IOException::class)
        fun deserialize(data: ByteArray): KdfParameters? {
            val inputStream = LittleEndianDataInputStream(ByteArrayInputStream(data))
            val dictionary = deserialize(inputStream)

            val uuidBytes = dictionary.getByteArray(PARAM_UUID) ?: return null
            val uuid = bytes16ToUuid(uuidBytes)

            val kdfParameters = KdfParameters(uuid)
            kdfParameters.copyTo(dictionary)
            return kdfParameters
        }

        @Throws(IOException::class)
        fun serialize(kdfParameters: KdfParameters): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val outputStream = LittleEndianDataOutputStream(byteArrayOutputStream)

            serialize(kdfParameters, outputStream)

            return byteArrayOutputStream.toByteArray()
        }
    }

}
