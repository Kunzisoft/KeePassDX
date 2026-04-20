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

import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.Arrays

object CharArrayUtil {
    fun CharArray.removeSpaceChars(): CharArray {
        var count = 0
        for (c in this) {
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t' && c != '\u00A0') {
                count++
            }
        }
        val result = CharArray(count)
        var j = 0
        for (c in this) {
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t' && c != '\u00A0') {
                result[j++] = c
            }
        }
        return result
    }

    fun CharArray.toByteArray(charset: Charset): ByteArray {
        val charBuffer = CharBuffer.wrap(this)
        val byteBuffer = charset.encode(charBuffer)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }

    fun CharArray.toUtf8ByteArray(): ByteArray {
        return this.toByteArray(Charset.forName("UTF-8"))
    }

    fun CharArray.clear() {
        Arrays.fill(this, '\u0000')
    }
}
