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
package com.kunzisoft.keepass.database.crypto

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.encrypt.StreamCipher

enum class CrsAlgorithm(val id: UnsignedInt) {

    Null(UnsignedInt(0)),
    ArcFourVariant(UnsignedInt(1)),
    Salsa20(UnsignedInt(2)),
    ChaCha20(UnsignedInt(3));

    companion object {

        @Throws(Exception::class)
        fun getCipher(algorithm: CrsAlgorithm?, key: ByteArray): StreamCipher {
            return when (algorithm) {
                Salsa20 -> HashManager.getSalsa20(key)
                ChaCha20 -> HashManager.getChaCha20(key)
                else -> throw Exception("Invalid random cipher")
            }
        }

        fun fromId(num: UnsignedInt): CrsAlgorithm? {
            for (e in values()) {
                if (e.id == num) {
                    return e
                }
            }
            return null
        }
    }

}
