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

import com.kunzisoft.keepass.utils.readBytes4ToUInt
import com.kunzisoft.keepass.utils.readBytesLength
import java.io.IOException
import java.io.InputStream

class DatabaseHeaderKDB : DatabaseHeader() {

    /**
     * Used for the dwKeyEncRounds AES transformations
     */
    var transformSeed = ByteArray(32)

    var signature1: UInt = 0u                  // = DBSIG_1
    var signature2: UInt = 0u                  // = DBSIG_2
    var flags: UInt = 0u
    var version: UInt = 0u

    /** Number of groups in the database  */
    var numGroups: UInt = 0u
    /** Number of entries in the database  */
    var numEntries: UInt = 0u

    /**
     * SHA-256 hash of the database, used for integrity check
     */
    var contentsHash = ByteArray(32)

    // As UInt
    var numKeyEncRounds: UInt = 0u

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
        val DBSIG_1: UInt = 0x9AA2D903u
        val DBSIG_2: UInt = 0xB54BFB65u
        val DBVER_DW: UInt = 0x00030004u

        val FLAG_SHA2: UInt = 1u
        val FLAG_RIJNDAEL: UInt = 2u
        val FLAG_ARCFOUR: UInt = 4u
        val FLAG_TWOFISH: UInt = 8u

        /** Size of byte buffer needed to hold this struct.  */
        const val BUF_SIZE = 124

        fun matchesHeader(
            sig1: UInt,
            sig2: UInt,
        ): Boolean {
            return sig1 == DBSIG_1 && sig2 == DBSIG_2
        }

        fun compatibleHeaders(
            one: UInt,
            two: UInt,
        ): Boolean {
            return one.toInt() and -0x100 == two.toInt() and -0x100
        }
    }


}
