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
package com.kunzisoft.keepass.utils

import java.nio.ByteBuffer
import java.util.UUID

object UUIDUtils {

    /**
     * Specific UUID CharArray format for KeePass database
     */
    fun UUID.asHexCharArray(): CharArray? {
        try {
            val buf = uuidTo16Bytes(this)

            val len = buf.size
            if (len == 0) {
                return charArrayOf()
            }

            val result = CharArray(len * 2)

            var bt: Short
            var high: Char
            var low: Char
            for (i in buf.indices) {
                bt = (buf[i].toInt() and 0xFF).toShort()
                high = (bt.toInt() ushr 4).toChar()
                low = (bt.toInt() and 0x0F).toChar()
                result[i * 2] = byteToChar(high)
                result[i * 2 + 1] = byteToChar(low)
            }

            return result
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Specific UUID string format for KeePass database
     */
    fun UUID.asHexString(): String? {
        return asHexCharArray()?.let { String(it) }
    }

    /**
     * From a specific UUID KeePass database format,
     * Note : For a standard UUID string format, use UUID.fromString()
     */
    fun CharArray.asUUID(): UUID? {
        if (this.size != 32) return null

        val charArray = this.map { it.lowercaseChar() }
        val leastSignificantChars = CharArray(16)
        val mostSignificantChars = CharArray(16)

        var i = 31
        while (i >= 0) {
            if (i >= 16) {
                mostSignificantChars[32 - i] = charArray[i]
                mostSignificantChars[31 - i] = charArray[i - 1]
            } else {
                leastSignificantChars[16 - i] = charArray[i]
                leastSignificantChars[15 - i] = charArray[i - 1]
            }
            i -= 2
        }
        val standardUUIDString = StringBuilder()
        standardUUIDString.append(leastSignificantChars)
        standardUUIDString.append(mostSignificantChars)
        standardUUIDString.insert(8, '-')
        standardUUIDString.insert(13, '-')
        standardUUIDString.insert(18, '-')
        standardUUIDString.insert(23, '-')
        return try {
            UUID.fromString(standardUUIDString.toString())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * From a specific UUID KeePass database string format,
     * Note : For a standard UUID string format, use UUID.fromString()
     */
    fun String.asUUID(): UUID? {
        return this.toCharArray().asUUID()
    }

    fun ByteArray.asUUID(): UUID {
        val bb = ByteBuffer.wrap(this)
        val firstLong = bb.getLong()
        val secondLong = bb.getLong()
        return UUID(firstLong, secondLong)
    }

    fun UUID.asBytes(): ByteArray {
        return ByteBuffer.allocate(16).apply {
            putLong(mostSignificantBits)
            putLong(leastSignificantBits)
        }.array()
    }

    // Use short to represent unsigned byte
    private fun byteToChar(bt: Char): Char {
        return if (bt.code >= 10) {
            ('A'.code + bt.code - 10).toChar()
        } else {
            ('0'.code + bt.code).toChar()
        }
    }
}
