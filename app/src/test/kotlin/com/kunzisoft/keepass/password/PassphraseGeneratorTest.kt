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

package com.kunzisoft.keepass.password

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassphraseGeneratorTest {

    private val generator = PassphraseGenerator()

    @Test
    fun testGeneratePassphraseLower() {
        val wordCount = 3
        val separator = "-"
        val result = generator.generatePassphrase(
            wordCount,
            separator,
            PassphraseGenerator.WordCase.LOWER_CASE,
            PassphraseGenerator.SeparatorType.CUSTOM_VALUE,
            0
        )
        assertTrue(result.isNotEmpty())
        val resultStr = String(result)
        val words = resultStr.split(separator)
        assertEquals(wordCount, words.size)
        words.forEach { word ->
            assertEquals(word.lowercase(), word)
        }
    }

    @Test
    fun testGeneratePassphraseUpper() {
        val wordCount = 2
        val separator = "_"
        val result = generator.generatePassphrase(
            wordCount,
            separator,
            PassphraseGenerator.WordCase.UPPER_CASE,
            PassphraseGenerator.SeparatorType.CUSTOM_VALUE,
            0
        )
        val resultStr = String(result)
        val words = resultStr.split(separator)
        assertEquals(wordCount, words.size)
        words.forEach { word ->
            assertEquals(word.uppercase(), word)
        }
    }

    @Test
    fun testGeneratePassphraseTitle() {
        val wordCount = 2
        val separator = "."
        val result = generator.generatePassphrase(
            wordCount,
            separator,
            PassphraseGenerator.WordCase.TITLE_CASE,
            PassphraseGenerator.SeparatorType.CUSTOM_VALUE,
            0
        )
        val resultStr = String(result)
        val words = resultStr.split(separator)
        assertEquals(wordCount, words.size)
        words.forEach { word ->
            assertTrue(word[0].isUpperCase())
            if (word.length > 1) {
                assertEquals(word.substring(1).lowercase(), word.substring(1))
            }
        }
    }

    @Test
    fun testGeneratePassphraseRandomDigits() {
        val wordCount = 3
        val digitsCount = 2
        val result = generator.generatePassphrase(
            wordCount,
            "-",
            PassphraseGenerator.WordCase.LOWER_CASE,
            PassphraseGenerator.SeparatorType.RANDOM_NUMBERS,
            digitsCount
        )
        val resultStr = String(result)
        // Internal TEMP_SPLIT is "-"
        assertFalse("Should not contain internal separator", resultStr.contains("-"))
        
        // Count digits. There should be (wordCount - 1) * digitsCount digits.
        val digits = resultStr.filter { it.isDigit() }
        assertEquals((wordCount - 1) * digitsCount, digits.length)
        
        // Ensure no '0' is used as per implementation
        assertFalse("Should not contain 0", resultStr.contains("0"))
    }
}
