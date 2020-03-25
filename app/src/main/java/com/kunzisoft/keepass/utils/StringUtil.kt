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
package com.kunzisoft.keepass.utils

import java.util.*

object StringUtil {

    /**
     * Create a list of String by split text when ' ', '\t', '\r' or '\n' is found
     */
    fun splitStringTerms(text: String?): List<String> {
        val list = ArrayList<String>()
        if (text == null) {
            return list
        }

        val stringBuilder = StringBuilder()
        var quoted = false

        for (element in text) {

            if ((element == ' ' || element == '\t' || element == '\r' || element == '\n') && !quoted) {

                val len = stringBuilder.length
                when {
                    len > 0 -> {
                        list.add(stringBuilder.toString())
                        stringBuilder.delete(0, len)
                    }
                    element == '\"' -> quoted = !quoted
                    else -> stringBuilder.append(element)
                }
            }
        }

        if (stringBuilder.isNotEmpty()) {
            list.add(stringBuilder.toString())
        }

        return list
    }

    fun indexOfIgnoreCase(text: String, search: String, start: Int, locale: Locale): Int {
        return text.toLowerCase(locale).indexOf(search.toLowerCase(locale), start)
    }

    fun indexOfIgnoreCase(text: String, search: String, locale: Locale): Int {
        return indexOfIgnoreCase(text, search, 0, locale)
    }

    fun replaceAllIgnoresCase(text: String, find: String, newText: String, locale: Locale): String {
        var currentText = text
        var pos = 0
        while (pos < currentText.length) {
            pos = indexOfIgnoreCase(currentText, find, pos, locale)
            if (pos < 0) {
                break
            }

            val before = currentText.substring(0, pos)
            val after = currentText.substring(pos + find.length)

            currentText = before + newText + after
            pos += newText.length
        }

        return currentText
    }
}

fun UUID.toKeePassRefString(): String {
    val tempString = toString().replace("-", "").toUpperCase(Locale.ENGLISH)
    return StringBuffer(reverseString2(tempString.substring(12, 16)))
            .append(reverseString2(tempString.substring(8, 12)))
            .append(reverseString2(tempString.substring(0, 8)))
            .append(reverseString2(tempString.substring(20, 32)))
            .append(reverseString2(tempString.substring(16, 20))).toString()
}

private fun reverseString2(string: String): String {
    return string.chunked(2).reversed().joinToString("")
}
