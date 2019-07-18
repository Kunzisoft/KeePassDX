/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.password

import android.content.res.Resources
import com.kunzisoft.keepass.R
import java.security.SecureRandom

class PasswordGenerator(private val resources: Resources) {

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
                         extended: Boolean): String {
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

        val characterSet = getCharacterSet(
                upperCase,
                lowerCase,
                digits,
                minus,
                underline,
                space,
                specials,
                brackets,
                extended)

        val size = characterSet.length

        val buffer = StringBuilder()

        val random = SecureRandom() // use more secure variant of Random!
        if (size > 0) {
            for (i in 0 until length) {
                buffer.append(characterSet[random.nextInt(size)])
            }
        }
        return buffer.toString()
    }

    private fun getCharacterSet(upperCase: Boolean,
                                lowerCase: Boolean,
                                digits: Boolean,
                                minus: Boolean,
                                underline: Boolean,
                                space: Boolean,
                                specials: Boolean,
                                brackets: Boolean,
                                extended: Boolean): String {
        val charSet = StringBuilder()

        if (upperCase) {
            charSet.append(UPPERCASE_CHARS)
        }

        if (lowerCase) {
            charSet.append(LOWERCASE_CHARS)
        }

        if (digits) {
            charSet.append(DIGIT_CHARS)
        }

        if (minus) {
            charSet.append(MINUS_CHAR)
        }

        if (underline) {
            charSet.append(UNDERLINE_CHAR)
        }

        if (space) {
            charSet.append(SPACE_CHAR)
        }

        if (specials) {
            charSet.append(SPECIAL_CHARS)
        }

        if (brackets) {
            charSet.append(BRACKET_CHARS)
        }

        if (extended) {
            charSet.append(extendedChars())
        }

        return charSet.toString()
    }

    companion object {
        private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val DIGIT_CHARS = "0123456789"
        private const val MINUS_CHAR = "-"
        private const val UNDERLINE_CHAR = "_"
        private const val SPACE_CHAR = " "
        private const val SPECIAL_CHARS = "!\"#$%&'*+,./:;=?@\\^`"
        private const val BRACKET_CHARS = "[]{}()<>"
    }
}
