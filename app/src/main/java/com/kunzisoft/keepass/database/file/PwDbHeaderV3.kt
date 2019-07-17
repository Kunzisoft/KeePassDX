/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX. Derived from KeePass for J2ME
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
 *
 */

package com.kunzisoft.keepass.database.file

import com.kunzisoft.keepass.stream.LEDataInputStream

import java.io.IOException

class PwDbHeaderV3 : PwDbHeader() {

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

    var numKeyEncRounds: Int = 0

    /**
     * Parse given buf, as read from file.
     * @param buf
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadFromFile(buf: ByteArray, offset: Int) {
        signature1 = LEDataInputStream.readInt(buf, offset)
        signature2 = LEDataInputStream.readInt(buf, offset + 4)
        flags = LEDataInputStream.readInt(buf, offset + 8)
        version = LEDataInputStream.readInt(buf, offset + 12)

        System.arraycopy(buf, offset + 16, masterSeed, 0, 16)
        System.arraycopy(buf, offset + 32, encryptionIV, 0, 16)

        numGroups = LEDataInputStream.readInt(buf, offset + 48)
        numEntries = LEDataInputStream.readInt(buf, offset + 52)

        System.arraycopy(buf, offset + 56, contentsHash, 0, 32)

        System.arraycopy(buf, offset + 88, transformSeed, 0, 32)
        numKeyEncRounds = LEDataInputStream.readInt(buf, offset + 120)
        if (numKeyEncRounds < 0) {
            // TODO: Really treat this like an unsigned integer
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
