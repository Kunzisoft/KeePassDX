/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX. Derived from KeePass for J2ME
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
 *
 */

package com.kunzisoft.keepass.database.file

import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.readBytes4ToUInt
import com.kunzisoft.keepass.utils.readBytesLength
import java.io.IOException
import java.io.InputStream

class DatabaseHeaderKDB : DatabaseHeader() {

    /**
     * Used for the dwKeyEncRounds AES transformations
     */
    var transformSeed = ByteArray(32)

    var signature1 = UnsignedInt(0)                  // = DBSIG_1
    var signature2 = UnsignedInt(0)                  // = DBSIG_2
    var flags= UnsignedInt(0)
    var version= UnsignedInt(0)

    /** Number of groups in the database  */
    var numGroups = UnsignedInt(0)
    /** Number of entries in the database  */
    var numEntries = UnsignedInt(0)

    /**
     * SHA-256 hash of the database, used for integrity check
     */
    var contentsHash = ByteArray(32)

    // As UInt
    var numKeyEncRounds = UnsignedInt(0)

    /**
     * Parse given buf, as read from file.
     */
    @Throws(IOException::class)
    fun loadFromFile(inputStream: InputStream) {
        signature1 = inputStream.readBytes4ToUInt() // 4 bytes
        signature2 = inputStream.readBytes4ToUInt() // 4 bytes
        flags = inputStream.readBytes4ToUInt() // 4 bytes
        version = inputStream.readBytes4ToUInt() // 4 bytes
        masterSeed = inputStream.readBytesLength(16) // 16 bytes
        encryptionIV = inputStream.readBytesLength(16) // 16 bytes
        numGroups = inputStream.readBytes4ToUInt() // 4 bytes
        numEntries = inputStream.readBytes4ToUInt() // 4 bytes
        contentsHash = inputStream.readBytesLength(32) // 32 bytes
        transformSeed = inputStream.readBytesLength(32) // 32 bytes
        numKeyEncRounds = inputStream.readBytes4ToUInt()
    }

    init {
        masterSeed = ByteArray(16)
    }

    /** Determine if the database version is compatible with this application
     * @return true, if it is compatible
     */
    fun matchesVersion(): Boolean {
        return compatibleHeaders(version, DBVER_DW)
    }

    companion object {

        // DB sig from KeePass 1.03
        val DBSIG_1 = UnsignedInt(-0x655d26fd) // 0x9AA2D903
        val DBSIG_2 = UnsignedInt(-0x4ab4049b) // 0xB54BFB65
        val DBVER_DW = UnsignedInt(0x00030004)

        val FLAG_SHA2 = UnsignedInt(1)
        val FLAG_RIJNDAEL = UnsignedInt(2)
        val FLAG_ARCFOUR = UnsignedInt(4)
        val FLAG_TWOFISH = UnsignedInt(8)

        /** Size of byte buffer needed to hold this struct.  */
        const val BUF_SIZE = 124

        fun matchesHeader(sig1: UnsignedInt, sig2: UnsignedInt): Boolean {
            return sig1.toKotlinInt() == DBSIG_1.toKotlinInt() && sig2.toKotlinInt() == DBSIG_2.toKotlinInt()
        }

        fun compatibleHeaders(one: UnsignedInt, two: UnsignedInt): Boolean {
            return one.toKotlinInt() and -0x100 == two.toKotlinInt() and -0x100
        }
    }


}
