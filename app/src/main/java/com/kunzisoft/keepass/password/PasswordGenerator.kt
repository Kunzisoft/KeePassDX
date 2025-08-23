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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableString
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.ReplacementSpan
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
                         considerChars: String,
                         ignoreChars: String,
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
            && !extended
            && considerChars.isEmpty()) {
            throw IllegalArgumentException(resources.getString(R.string.error_pass_gen_type))
        }

        // Filter builder
        val passwordFilters = PasswordFilters().apply {
            this.length = length
            this.ignoreChars = ignoreChars
            if (excludeAmbiguousChar)
                this.ignoreChars += AMBIGUOUS_CHARS
            if (upperCase) {
                addFilter(
                    UPPERCASE_CHARS,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (lowerCase) {
                addFilter(
                    LOWERCASE_CHARS,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (digits) {
                addFilter(
                    DIGIT_CHARS,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (minus) {
                addFilter(
                    MINUS_CHAR,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (underline) {
                addFilter(
                    UNDERLINE_CHAR,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (space) {
                addFilter(
                    SPACE_CHAR,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (specials) {
                addFilter(
                    SPECIAL_CHARS,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (brackets) {
                addFilter(
                    BRACKET_CHARS,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (extended) {
                addFilter(
                    extendedChars,
                    if (atLeastOneFromEach) 1 else 0
                )
            }
            if (considerChars.isNotEmpty()) {
                addFilter(
                    considerChars,
                    if (atLeastOneFromEach) 1 else 0
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
            var selectableChars: String = if (requiredCharactersLeft < passwordFilters.length - i) {
                // choose from any group at random
                passwordFilters.getSelectableChars()
            } else {
                // choose only from a group that we need to satisfy a minimum for.
                passwordFilters.getSelectableCharsForNeed()
            }
            passwordFilters.ignoreChars.forEach {
                selectableChars = selectableChars.replace(it.toString(), "")
            }

            // Now that the string is built, get the next random character.
            val selectableCharsMaxIndex = selectableChars.length
            val randomSelectableCharsIndex = if (selectableCharsMaxIndex > 0) random.nextInt(selectableCharsMaxIndex) else 0
            val nextChar = selectableChars[randomSelectableCharsIndex]

            // Put at random position
            val randomStringMaxIndex = randomString.length
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
        var ignoreChars = ""
        val filters = mutableListOf<Filter>()

        fun addFilter(chars: String, minCharsNeeded: Int) {
            filters.add(Filter(chars, minCharsNeeded))
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

    companion object {
        private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val DIGIT_CHARS = "0123456789"
        private const val MINUS_CHAR = "-"
        private const val UNDERLINE_CHAR = "_"
        private const val SPACE_CHAR = " "
        private const val SPECIAL_CHARS = "!\"#$%&'*+,./:;=?@\\^`"
        private const val BRACKET_CHARS = "[]{}()<>"
        private const val AMBIGUOUS_CHARS = "iI|lLoO01"

        // From KeePassXC code https://github.com/keepassxreboot/keepassxc/pull/538
        private val extendedChars = run {
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
            charSet.toString()
        }

        // span to make a character look like a different one
        private class CharacterSkinSpan(val color: Int, val replacement: String) : ReplacementSpan() {
            override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
                return paint.measureText(replacement).toInt()
            }
            override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
                paint.color = color
                canvas.drawText(replacement, x, y.toFloat(), paint)
            }
        }

        private fun getStyleForChar(char: Char): android.text.style.CharacterStyle? {
            return when {
                char == ' '                   -> CharacterSkinSpan(Color.rgb(80, 80, 80), "•") // GRAY "•"
                DIGIT_CHARS   .contains(char) -> ForegroundColorSpan(Color.rgb(246,  79,  62)) // RED
                SPECIAL_CHARS .contains(char) -> ForegroundColorSpan(Color.rgb( 39, 166, 228)) // BLUE
                BRACKET_CHARS .contains(char) -> ForegroundColorSpan(Color.rgb(185,  38, 209)) // PURPLE
                MINUS_CHAR    .contains(char) -> ForegroundColorSpan(Color.rgb(185,  38, 209)) // PURPLE
                UNDERLINE_CHAR.contains(char) -> ForegroundColorSpan(Color.rgb(185,  38, 209)) // PURPLE
                extendedChars .contains(char) -> ForegroundColorSpan(Color.rgb( 44, 181,  50)) // GREEN
                else -> null
            }
        }

        // filter that is called whenever the password text changes to style the new characters
        val passwordStylingInputFilter = object: android.text.InputFilter {
            override fun filter(source: CharSequence, start: Int, end: Int, dest: android.text.Spanned, dstart: Int, dend: Int): CharSequence? {
                // clear existing spans, only use given range
                val src = source.toString().substring(start, end)
                val spannable = SpannableString(src)

                // apply styling per character
                spannable.forEachIndexed { i, char ->
                    getStyleForChar(char)?.let { charStyle ->
                        spannable.setSpan(charStyle, i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                return spannable
            }
        }

    }
}
