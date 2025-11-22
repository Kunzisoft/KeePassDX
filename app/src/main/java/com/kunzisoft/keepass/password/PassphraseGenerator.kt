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

import me.gosimple.nbvcxz.resources.Generator
import java.security.SecureRandom

class PassphraseGenerator {

    @Throws(IllegalArgumentException::class)
    fun generatePassphrase(wordCount: Int,
                           wordSeparator: String,
                           wordCase: WordCase,
                           separatorType: SeparatorType,
                           randomDigitsCount: Int): String {
        val effectiveSeparator = if (separatorType == SeparatorType.RANDOM_NUMBERS) TEMP_SPLIT else wordSeparator

        // From eff_large dictionary
        val passphrase = when (wordCase) {
            WordCase.LOWER_CASE -> {
                Generator.generatePassphrase(effectiveSeparator, wordCount)
            }
            WordCase.UPPER_CASE -> {
                applyWordCase(wordCount, effectiveSeparator) { word ->
                    word.uppercase()
                }
            }
            WordCase.TITLE_CASE -> {
                applyWordCase(wordCount, effectiveSeparator) { word ->
                    word.replaceFirstChar { char -> char.uppercaseChar() }
                }
            }
        }

        return if (separatorType == SeparatorType.RANDOM_NUMBERS) {
            replaceSeparatorWithRandomNumbers(passphrase, effectiveSeparator, randomDigitsCount)
        } else {
            passphrase
        }
    }

    private fun applyWordCase(wordCount: Int,
                              wordSeparator: String,
                              wordAction: (word: String) -> String): String {
        val splitWords = Generator.generatePassphrase(TEMP_SPLIT, wordCount).split(TEMP_SPLIT)
        val stringBuilder = StringBuilder()
        splitWords.forEach {
            stringBuilder
                .append(wordAction(it))
                .append(wordSeparator)
        }
        return stringBuilder.toString().removeSuffix(wordSeparator)
    }

    private fun replaceSeparatorWithRandomNumbers(passphrase: String,
                                                  separator: String,
                                                  digitsCount: Int): String {
        val random = SecureRandom()
        val splitWords = passphrase.split(separator)
        val stringBuilder = StringBuilder()

        splitWords.forEachIndexed { index, word ->
            stringBuilder.append(word)
            if (index < splitWords.size - 1) {
                repeat(digitsCount) {
                    // Use only digits 1-9 to avoid ambiguity between 0 and O
                    stringBuilder.append(random.nextInt(9) + 1)A
                }
            }
        }

        return stringBuilder.toString()
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

    enum class SeparatorType {
        CUSTOM_VALUE,
        RANDOM_NUMBERS;

        fun toPreferenceString(): String {
            return when (this) {
                RANDOM_NUMBERS -> "random_numbers"
                CUSTOM_VALUE -> "custom_value"
            }
        }

        companion object {
            fun fromString(value: String): SeparatorType {
                return when (value) {
                    "random_numbers" -> RANDOM_NUMBERS
                    else -> CUSTOM_VALUE
                }
            }
        }
    }

    companion object {
        private const val TEMP_SPLIT = "-"
    }
}
