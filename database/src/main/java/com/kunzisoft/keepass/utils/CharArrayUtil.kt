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

    fun CharArray.contentEquals(other: CharArray, ignoreCase: Boolean = false): Boolean {
        if (this.size != other.size) return false
        for (i in indices) {
            if (!this[i].equals(other[i], ignoreCase)) {
                return false
            }
        }
        return true
    }

    fun CharArray.indexOf(str: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
        if (str.isEmpty()) return startIndex
        val firstChar = str[0]
        val max = size - str.length
        for (i in startIndex..max) {
            if (this[i].equals(firstChar, ignoreCase)) {
                var match = true
                for (j in 1 until str.length) {
                    if (!this[i + j].equals(str[j], ignoreCase)) {
                        match = false
                        break
                    }
                }
                if (match) return i
            }
        }
        return -1
    }

    fun CharArray.contains(str: String, ignoreCase: Boolean = false): Boolean {
        return indexOf(str, 0, ignoreCase) >= 0
    }

    fun CharArray.replace(oldValue: String, newValue: CharArray, ignoreCase: Boolean = false): CharArray {
        var occurrences = 0
        var index = indexOf(oldValue, 0, ignoreCase)
        while (index != -1) {
            occurrences++
            index = indexOf(oldValue, index + oldValue.length, ignoreCase)
        }

        if (occurrences == 0) return this

        val newSize = size + (newValue.size - oldValue.length) * occurrences
        val result = CharArray(newSize)
        var resultPos = 0
        var currentPos = 0

        index = indexOf(oldValue, 0, ignoreCase)
        while (index != -1) {
            val copyLen = index - currentPos
            System.arraycopy(this, currentPos, result, resultPos, copyLen)
            resultPos += copyLen

            System.arraycopy(newValue, 0, result, resultPos, newValue.size)
            resultPos += newValue.size

            currentPos = index + oldValue.length
            index = indexOf(oldValue, currentPos, ignoreCase)
        }

        System.arraycopy(this, currentPos, result, resultPos, size - currentPos)
        return result
    }
}
