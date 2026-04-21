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
 */

package com.kunzisoft.keepass.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrayUtilTest {

    @Test
    fun testContains() {
        val label = "This is a test label".toCharArray()

        assertTrue(label.contains("test"))
        assertTrue(label.contains("This"))
        assertTrue(label.contains("label"))
        assertTrue(label.contains(""))
        
        assertFalse(label.contains("notfound"))
        assertFalse(label.contains("TEST"))
    }

    @Test
    fun testContainsIgnoreCase() {
        val label = "This is a test label".toCharArray()

        assertTrue(label.contains("test", true))
        assertTrue(label.contains("TEST", true))
        assertTrue(label.contains("THIS", true))
        assertTrue(label.contains("LABEL", true))
        
        assertFalse(label.contains("notfound", true))
    }

    @Test
    fun testContainsEdgeCases() {
        val emptyLabel = charArrayOf()
        assertTrue(emptyLabel.contains(""))
        assertFalse(emptyLabel.contains("a"))

        val smallLabel = "abc".toCharArray()
        assertTrue(smallLabel.contains("abc"))
        assertFalse(smallLabel.contains("abcd"))
    }
}
