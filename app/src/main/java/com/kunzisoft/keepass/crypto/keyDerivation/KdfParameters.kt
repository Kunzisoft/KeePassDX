/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto.keyDerivation

import com.kunzisoft.keepass.utils.VariantDictionary
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class KdfParameters internal constructor(val uuid: UUID) : VariantDictionary() {

    fun setParamUUID() {
        setByteArray(PARAM_UUID, DatabaseInputOutputUtils.uuidToBytes(uuid))
    }

    companion object {

        private const val PARAM_UUID = "\$UUID"

        @Throws(IOException::class)
        fun deserialize(data: ByteArray): KdfParameters? {
            val bis = ByteArrayInputStream(data)
            val lis = LEDataInputStream(bis)

            val d = deserialize(lis) ?: return null

            val uuid = DatabaseInputOutputUtils.bytesToUuid(d.getByteArray(PARAM_UUID))

            val kdfP = KdfParameters(uuid)
            kdfP.copyTo(d)
            return kdfP
        }

        @Throws(IOException::class)
        fun serialize(kdf: KdfParameters): ByteArray {
            val bos = ByteArrayOutputStream()
            val los = LEDataOutputStream(bos)

            serialize(kdf, los)

            return bos.toByteArray()
        }
    }

}
