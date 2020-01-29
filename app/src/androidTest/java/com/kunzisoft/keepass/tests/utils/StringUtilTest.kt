/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.tests.utils

import java.util.Locale

import com.kunzisoft.keepass.utils.StringUtil

import junit.framework.TestCase

class StringUtilTest : TestCase() {
    private val text = "AbCdEfGhIj"
    private val search = "BcDe"
    private val badSearch = "Ed"

    private val repText = "AbCtestingaBc"
    private val repSearch = "ABc"
    private val repSearchBad = "CCCCCC"
    private val repNew = "12345"
    private val repResult = "12345testing12345"

    fun testIndexOfIgnoreCase1() {
        assertEquals(1, StringUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH))
    }

    fun testIndexOfIgnoreCase2() {
        assertEquals(-1f, StringUtil.indexOfIgnoreCase(text, search, Locale.ENGLISH).toFloat(), 2f)
    }

    fun testIndexOfIgnoreCase3() {
        assertEquals(-1, StringUtil.indexOfIgnoreCase(text, badSearch, Locale.ENGLISH))
    }

    fun testReplaceAllIgnoresCase1() {
        assertEquals(repResult, StringUtil.replaceAllIgnoresCase(repText, repSearch, repNew, Locale.ENGLISH))
    }

    fun testReplaceAllIgnoresCase2() {
        assertEquals(repText, StringUtil.replaceAllIgnoresCase(repText, repSearchBad, repNew, Locale.ENGLISH))
    }
}
