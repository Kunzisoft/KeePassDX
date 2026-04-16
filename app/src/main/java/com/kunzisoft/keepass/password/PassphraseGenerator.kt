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
package com.kunzisoft.keepass.password

import com.kunzisoft.keepass.utils.clear
import me.gosimple.nbvcxz.resources.Generator

class PassphraseGenerator {

    @Throws(IllegalArgumentException::class)
    fun generatePassphrase(
        wordCount: Int,
        wordSeparator: String,
        wordCase: WordCase
    ): CharArray {
        // From eff_large dictionary
        return when (wordCase) {
            WordCase.LOWER_CASE -> {
                // TODO Ask the lib to directly retrieve a CharArray
                Generator.generatePassphrase(wordSeparator, wordCount).toCharArray()
            }
            WordCase.UPPER_CASE -> {
                applyWordCase(wordCount, wordSeparator) { word ->
                    word.uppercase()
                }
            }
            WordCase.TITLE_CASE -> {
                applyWordCase(wordCount, wordSeparator) { word ->
                    word.replaceFirstChar { char -> char.uppercaseChar() }
                }
            }
        }
    }

    private fun applyWordCase(
        wordCount: Int,
        wordSeparator: String,
        wordAction: (word: String) -> String
    ): CharArray {
        val splitWords = Generator.generatePassphrase(TEMP_SPLIT, wordCount).split(TEMP_SPLIT)
        val stringBuilder = StringBuilder()
        splitWords.forEachIndexed { index, word ->
            stringBuilder.append(wordAction(word))
            if (index < splitWords.size - 1) {
                stringBuilder.append(wordSeparator)
            }
        }
        val result = CharArray(stringBuilder.length)
        stringBuilder.getChars(0, stringBuilder.length, result, 0)
        stringBuilder.clear()
        
        return result
    }

    enum class WordCase {
        LOWER_CASE,
        UPPER_CASE,
        TITLE_CASE;

        companion object {
            fun getByOrdinal(position: Int): WordCase {
                return when (position) {
                    0 -> LOWER_CASE
                    1 -> UPPER_CASE
                    else -> TITLE_CASE
                }
            }
        }
    }

    companion object {
        private const val TEMP_SPLIT = "-"
    }
}
