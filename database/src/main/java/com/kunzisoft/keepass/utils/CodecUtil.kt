/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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

object CodecUtil {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val BASE32_DECODE_TABLE = IntArray(128) { -1 }

    init {
        for (i in BASE32_ALPHABET.indices) {
            BASE32_DECODE_TABLE[BASE32_ALPHABET[i].code] = i
        }
    }

    /**
     * Decode a hex string (case insensitive) to a byte array.
     */
    fun decodeHex(hex: CharArray): ByteArray {
        if (hex.size % 2 != 0) {
            throw IllegalArgumentException("Hex string must have an even number of characters")
        }
        val result = ByteArray(hex.size / 2)
        for (i in result.indices) {
            val h = hex[i * 2]
            val l = hex[i * 2 + 1]
            result[i] = ((h.digitToInt(16) shl 4) or l.digitToInt(16)).toByte()
        }
        return result
    }

    /**
     * Decode a hex string (case insensitive) to a byte array.
     */
    fun decodeHex(hex: String): ByteArray {
        return decodeHex(hex.toCharArray())
    }

    /**
     * Decode a Base32 string (case insensitive, padding ignored) to a byte array.
     */
    fun decodeBase32(base32: CharArray): ByteArray {
        var cleanBase32 = base32.filter { it != '=' }.map { it.uppercaseChar() }
        if (cleanBase32.isEmpty()) return byteArrayOf()

        val length = cleanBase32.size
        val result = ByteArray(length * 5 / 8)
        var buffer = 0L
        var bitsLeft = 0
        var resultPos = 0

        for (c in cleanBase32) {
            val value = if (c.code < 128) BASE32_DECODE_TABLE[c.code] else -1
            if (value == -1) throw IllegalArgumentException("Invalid Base32 character: $c")
            
            buffer = (buffer shl 5) or value.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result[resultPos++] = (buffer shr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return result
    }

    /**
     * Encode a byte array to a Base32 CharArray (uppercase, no padding).
     */
    fun encodeBase32(data: ByteArray): CharArray {
        if (data.isEmpty()) return charArrayOf()
        val result = CharArray((data.size * 8 + 4) / 5)
        var buffer = 0L
        var bitsLeft = 0
        var resultPos = 0

        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF).toLong()
            bitsLeft += 8
            while (bitsLeft >= 5) {
                result[resultPos++] = BASE32_ALPHABET[(buffer shr (bitsLeft - 5) and 0x1F).toInt()]
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            result[resultPos++] = BASE32_ALPHABET[(buffer shl (5 - bitsLeft) and 0x1F).toInt()]
        }
        return result
    }
}
