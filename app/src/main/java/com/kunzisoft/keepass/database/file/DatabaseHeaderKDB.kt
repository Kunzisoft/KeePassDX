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

import com.kunzisoft.keepass.stream.bytes4ToInt
import com.kunzisoft.keepass.stream.readBytesLength
import com.kunzisoft.keepass.stream.readBytes4ToInt
import java.io.IOException
import java.io.InputStream

class DatabaseHeaderKDB : DatabaseHeader() {

    /**
     * Used for the dwKeyEncRounds AES transformations
     */
    var transformSeed = ByteArray(32)

    var signature1: Int = 0                  // = PWM_DBSIG_1
    var signature2: Int = 0                  // = DBSIG_2
    var flags: Int = 0
    var version: Int = 0

    /** Number of groups in the database  */
    var numGroups: Int = 0
    /** Number of entries in the database  */
    var numEntries: Int = 0

    /**
     * SHA-256 hash of the database, used for integrity check
     */
    var contentsHash = ByteArray(32)

    // As UInt
    var numKeyEncRounds: Int = 0

    /**
     * Parse given buf, as read from file.
     */
    @Throws(IOException::class)
    fun loadFromFile(inputStream: InputStream) {
        signature1 = inputStream.readBytes4ToInt() // 4 bytes
        signature2 = inputStream.readBytes4ToInt() // 4 bytes
        flags = inputStream.readBytes4ToInt() // 4 bytes
        version = inputStream.readBytes4ToInt() // 4 bytes
        masterSeed = inputStream.readBytesLength(16) // 16 bytes
        encryptionIV = inputStream.readBytesLength(16) // 16 bytes
        numGroups = inputStream.readBytes4ToInt() // 4 bytes
        numEntries = inputStream.readBytes4ToInt() // 4 bytes
        contentsHash = inputStream.readBytesLength(32) // 32 bytes
        transformSeed = inputStream.readBytesLength(32) // 32 bytes
        numKeyEncRounds = inputStream.readBytes4ToInt()
        if (numKeyEncRounds < 0) {
            // TODO: Really treat this like an unsigned integer #443
            throw IOException("Does not support more than " + Integer.MAX_VALUE + " rounds.")
        }
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
        const val DBSIG_2 = -0x4ab4049b
        // DB sig from KeePass 1.03
        const val DBVER_DW = 0x00030003

        const val FLAG_SHA2 = 1
        const val FLAG_RIJNDAEL = 2
        const val FLAG_ARCFOUR = 4
        const val FLAG_TWOFISH = 8

        /** Size of byte buffer needed to hold this struct.  */
        const val BUF_SIZE = 124

        fun matchesHeader(sig1: Int, sig2: Int): Boolean {
            return sig1 == PWM_DBSIG_1 && sig2 == DBSIG_2
        }

        fun compatibleHeaders(one: Int, two: Int): Boolean {
            return one and -0x100 == two and -0x100
        }
    }


}
