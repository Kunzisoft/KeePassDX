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

import com.kunzisoft.keepass.utils.CharArrayUtil.clear
import com.kunzisoft.keepass.utils.CharArrayUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.CharArrayUtil.toByteArray
import com.kunzisoft.keepass.utils.CharArrayUtil.toUtf8ByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class CharArrayUtilTest {

    @Test
    fun testRemoveSpaceChars() {
        val input = charArrayOf('a', ' ', 'b', '\n', 'c', '\r', 'd', '\t', 'e', '\u00A0', 'f')
        val expected = charArrayOf('a', 'b', 'c', 'd', 'e', 'f')
        assertArrayEquals(expected, input.removeSpaceChars())
        
        val emptyInput = charArrayOf()
        assertArrayEquals(charArrayOf(), emptyInput.removeSpaceChars())
        
        val onlySpaces = charArrayOf(' ', '\n', '\u00A0')
        assertArrayEquals(charArrayOf(), onlySpaces.removeSpaceChars())
    }

    @Test
    fun testToByteArray() {
        val input = charArrayOf('H', 'e', 'l', 'l', 'o')
        val charset = Charset.forName("UTF-8")
        val expected = "Hello".toByteArray(charset)
        assertArrayEquals(expected, input.toByteArray(charset))
    }

    @Test
    fun testToUtf8ByteArray() {
        val input = charArrayOf('W', 'o', 'r', 'l', 'd')
        val expected = "World".toByteArray(Charsets.UTF_8)
        assertArrayEquals(expected, input.toUtf8ByteArray())
    }

    @Test
    fun testClear() {
        val input = charArrayOf('S', 'e', 'c', 'r', 'e', 't')
        input.clear()
        for (c in input) {
            assertEquals('\u0000', c)
        }
    }
}
