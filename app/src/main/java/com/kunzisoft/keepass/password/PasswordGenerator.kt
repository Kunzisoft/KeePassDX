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

import android.content.res.Resources
import com.kunzisoft.keepass.R
import java.security.SecureRandom
import java.util.*

class PasswordGenerator(private val resources: Resources) {

    @Throws(IllegalArgumentException::class)
    fun generatePassword(length: Int,
                         upperCase: Boolean,
                         lowerCase: Boolean,
                         digits: Boolean,
                         minus: Boolean,
                         underline: Boolean,
                         space: Boolean,
                         specials: Boolean,
                         brackets: Boolean,
                         extended: Boolean,
                         atLeastOneFromEach: Boolean,
                         excludeAmbiguousChar: Boolean): String {
        // Desired password length is 0 or less
        if (length <= 0) {
            throw IllegalArgumentException(resources.getString(R.string.error_wrong_length))
        }

        // No option has been checked
        if (!upperCase
            && !lowerCase
            && !digits
            && !minus
            && !underline
            && !space
            && !specials
            && !brackets
            && !extended) {
            throw IllegalArgumentException(resources.getString(R.string.error_pass_gen_type))
        }

        // Filter builder
        val passwordFilters = PasswordFilters().apply {
            this.length = length
            if (upperCase) {
                addFilter(
                    Filter(
                        if (excludeAmbiguousChar) UPPERCASE_NON_AMBIGUOUS_CHARS else UPPERCASE_CHARS,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (lowerCase) {
                addFilter(
                    Filter(
                        if (excludeAmbiguousChar) LOWERCASE_NON_AMBIGUOUS_CHARS else LOWERCASE_CHARS,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (digits) {
                addFilter(
                    Filter(
                        if (excludeAmbiguousChar) DIGIT_NON_AMBIGUOUS_CHARS else DIGIT_CHARS,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (minus) {
                addFilter(
                    Filter(
                        MINUS_CHAR,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (underline) {
                addFilter(
                    Filter(
                        UNDERLINE_CHAR,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (space) {
                addFilter(
                    Filter(
                        SPACE_CHAR,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (specials) {
                addFilter(
                    Filter(
                        SPECIAL_CHARS,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (brackets) {
                addFilter(
                    Filter(
                        BRACKET_CHARS,
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
            if (extended) {
                addFilter(
                    Filter(
                        extendedChars(),
                        if (atLeastOneFromEach) 1 else 0
                    )
                )
            }
        }

        return generateRandomString(SecureRandom(), passwordFilters)
    }

    private fun generateRandomString(random: Random, passwordFilters: PasswordFilters): String {
        val randomString = StringBuilder()

        // Allocate appropriate memory for the password.
        var requiredCharactersLeft = passwordFilters.getRequiredCharactersLeft()

        // Build the password.
        for (i in 0 until passwordFilters.length) {
            val selectableChars: String = if (requiredCharactersLeft < passwordFilters.length - i) {
                // choose from any group at random
                passwordFilters.getSelectableChars()
            } else {
                // choose only from a group that we need to satisfy a minimum for.
                passwordFilters.getSelectableCharsForNeed()
            }

            // Now that the string is built, get the next random character.
            val selectableCharsMaxIndex = selectableChars.length - 1
            val randomSelectableCharsIndex = if (selectableCharsMaxIndex > 0) random.nextInt(selectableCharsMaxIndex) else 0
            val nextChar = selectableChars[randomSelectableCharsIndex]

            // Put at random position
            val randomStringMaxIndex = randomString.length - 1
            val randomStringIndex = if (randomStringMaxIndex > 0) random.nextInt(randomStringMaxIndex) else 0
            randomString.insert(randomStringIndex, nextChar)

            // Now figure out where it came from, and decrement the appropriate minimum value
            passwordFilters.getFilterThatContainsChar(nextChar)?.let {
                if (it.minCharsNeeded > 0) {
                    it.minCharsNeeded--
                    requiredCharactersLeft--
                }
            }
        }
        return randomString.toString()
    }

    private data class Filter(var chars: String,
                              var minCharsNeeded: Int)

    private class PasswordFilters {
        var length: Int = 0
        val filters = mutableListOf<Filter>()

        fun addFilter(filter: Filter) {
            filters.add(filter)
        }

        fun getRequiredCharactersLeft(): Int {
            var charsRequired = 0
            filters.forEach {
                charsRequired += it.minCharsNeeded
            }
            return charsRequired
        }

        fun getSelectableChars(): String {
            val stringBuilder = StringBuilder()
            filters.forEach {
                stringBuilder.append(it.chars)
            }
            return stringBuilder.toString()
        }

        fun getFilterThatContainsChar(char: Char): Filter? {
            return filters.find { it.chars.contains(char) }
        }

        fun getSelectableCharsForNeed(): String {
            val selectableChars = StringBuilder()
            // choose only from a group that we need to satisfy a minimum for.
            filters.forEach {
                if (it.minCharsNeeded > 0) {
                    selectableChars.append(it.chars)
                }
            }
            return selectableChars.toString()
        }
    }

    // From KeePassXC code https://github.com/keepassxreboot/keepassxc/pull/538
    private fun extendedChars(): String {
        val charSet = StringBuilder()
        // [U+0080, U+009F] are C1 control characters,
        // U+00A0 is non-breaking space
        run {
            var ch = '\u00A1'
            while (ch <= '\u00AC') {
                charSet.append(ch)
                ++ch
            }
        }
        // U+00AD is soft hyphen (format character)
        var ch = '\u00AE'
        while (ch < '\u00FF') {
            charSet.append(ch)
            ++ch
        }
        charSet.append('\u00FF')
        return charSet.toString()
    }

    companion object {
        private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val UPPERCASE_NON_AMBIGUOUS_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ"
        private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val LOWERCASE_NON_AMBIGUOUS_CHARS = "abcdefghjkmnpqrstuvwxyz"
        private const val DIGIT_CHARS = "0123456789"
        private const val DIGIT_NON_AMBIGUOUS_CHARS = "23456789"
        private const val MINUS_CHAR = "-"
        private const val UNDERLINE_CHAR = "_"
        private const val SPACE_CHAR = " "
        private const val SPECIAL_CHARS = "!\"#$%&'*+,./:;=?@\\^`"
        private const val BRACKET_CHARS = "[]{}()<>"
    }
}
