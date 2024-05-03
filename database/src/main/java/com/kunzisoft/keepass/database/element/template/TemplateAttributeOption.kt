/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.readStringStringMap
import com.kunzisoft.keepass.utils.writeStringStringMap

class TemplateAttributeOption() : Parcelable {

    private val mOptions: MutableMap<String, String> = mutableMapOf()

    constructor(parcel: Parcel) : this() {
        mOptions.apply {
            clear()
            putAll(parcel.readStringStringMap())
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringStringMap(mOptions)
    }

    override fun describeContents(): Int {
        return 0
    }

    var alias: String?
        get() {
            val tempAlias = mOptions[ALIAS_ATTR]
            if (tempAlias.isNullOrEmpty())
                return null
            return tempAlias
        }
        set(value) {
            if (value == null)
                mOptions.remove(ALIAS_ATTR)
            else
                mOptions[ALIAS_ATTR] = value
        }

    var default: String
        get() {
            return mOptions[DEFAULT_ATTR] ?: DEFAULT_VALUE
        }
        set(value) {
            mOptions[DEFAULT_ATTR] = value
        }

    fun getNumberChars(): Int {
        return try {
            if (mOptions[TEXT_NUMBER_CHARS_ATTR].equals(TEXT_NUMBER_CHARS_VALUE_MANY_STRING, true))
                TEXT_NUMBER_CHARS_VALUE_MANY
            else
                mOptions[TEXT_NUMBER_CHARS_ATTR]?.toInt() ?: TEXT_NUMBER_CHARS_VALUE_DEFAULT
        } catch (e: Exception) {
            TEXT_NUMBER_CHARS_VALUE_DEFAULT
        }
    }

    fun setNumberChars(numberChars: Int) {
        mOptions[TEXT_NUMBER_CHARS_ATTR] = numberChars.toString()
    }

    fun setNumberCharsToMany() {
        mOptions[TEXT_NUMBER_CHARS_ATTR] = TEXT_NUMBER_CHARS_VALUE_MANY_STRING
    }

    fun getNumberLines(): Int {
        return try {
            if (mOptions[TEXT_NUMBER_LINES_ATTR].equals(TEXT_NUMBER_LINES_VALUE_MANY_STRING, true))
                TEXT_NUMBER_LINES_VALUE_MANY
            else
                mOptions[TEXT_NUMBER_LINES_ATTR]?.toInt() ?: TEXT_NUMBER_LINES_VALUE_DEFAULT
        } catch (e: Exception) {
            TEXT_NUMBER_LINES_VALUE_DEFAULT
        }
    }

    fun setNumberLines(numberLines: Int) {
        val lines = if (numberLines == 0) 1 else numberLines
        mOptions[TEXT_NUMBER_LINES_ATTR] = lines.toString()
    }

    fun setNumberLinesToMany() {
        mOptions[TEXT_NUMBER_LINES_ATTR] = TEXT_NUMBER_LINES_VALUE_MANY_STRING
    }

    fun isLink(): Boolean {
        return try {
            mOptions[TEXT_LINK_ATTR]?.toBoolean() ?: TEXT_LINK_VALUE_DEFAULT
        } catch (e: Exception) {
            TEXT_LINK_VALUE_DEFAULT
        }
    }

    fun setLink(isLink: Boolean) {
        mOptions[TEXT_LINK_ATTR] = isLink.toString()
    }

    fun isAssociatedWithPasswordGenerator(): Boolean {
        return try {
            mOptions[PASSWORD_GENERATOR_ATTR]?.toBoolean() ?: PASSWORD_GENERATOR_VALUE_DEFAULT
        } catch (e: Exception) {
            PASSWORD_GENERATOR_VALUE_DEFAULT
        }
    }

    fun associatePasswordGenerator() {
        mOptions[PASSWORD_GENERATOR_ATTR] = true.toString()
    }

    fun getListItems(): List<String> {
        return mOptions[LIST_ITEMS]?.split(LIST_ITEMS_SEPARATOR) ?: listOf()
    }

    fun setListItems(vararg items: String) {
        mOptions[LIST_ITEMS] = items.joinToString(LIST_ITEMS_SEPARATOR)
    }

    fun getDateFormat(): DateInstant.Type {
        return when (mOptions[DATETIME_FORMAT_ATTR]) {
            DATETIME_FORMAT_VALUE_DATE -> DateInstant.Type.DATE
            DATETIME_FORMAT_VALUE_TIME -> DateInstant.Type.TIME
            else -> DateInstant.Type.DATE_TIME
        }
    }

    fun setDateFormatToDate() {
        mOptions[DATETIME_FORMAT_ATTR] = DATETIME_FORMAT_VALUE_DATE
    }

    fun setDateFormatToTime() {
        mOptions[DATETIME_FORMAT_ATTR] = DATETIME_FORMAT_VALUE_TIME
    }

    fun get(label: String): String? {
        return mOptions[label]
    }

    fun put(label: String, value: String) {
        mOptions[label] = value
    }

    fun remove(label: String) {
        mOptions.remove(label)
    }

    companion object CREATOR : Parcelable.Creator<TemplateAttributeOption> {
        override fun createFromParcel(parcel: Parcel): TemplateAttributeOption {
            return TemplateAttributeOption(parcel)
        }

        override fun newArray(size: Int): Array<TemplateAttributeOption?> {
            return arrayOfNulls(size)
        }

        /**
         * Applicable to each type
         * Define a text replacement for a label,
         * Useful to keep compatibility with old keepass apps by replacing standard field label
         */
        private const val ALIAS_ATTR = "alias"

        /**
         * Applicable to each type
         * Define a default string element representation
         * For a type LIST, represents a single string element representation
         */
        private const val DEFAULT_ATTR = "default"
        private const val DEFAULT_VALUE = ""

        /**
         * Applicable to type TEXT
         * Define a number of chars
         * Integer, can be "many" or "-1" to infinite value
         * "1" if not defined
         */
        private const val TEXT_NUMBER_CHARS_ATTR = "chars"
        private const val TEXT_NUMBER_CHARS_VALUE_MANY = -1
        private const val TEXT_NUMBER_CHARS_VALUE_MANY_STRING = "many"
        private const val TEXT_NUMBER_CHARS_VALUE_DEFAULT = -1

        /**
         * Applicable to type TEXT
         * Define a number of lines
         * Integer, can be "-1" to infinite value
         * "1" if not defined
         */
        private const val TEXT_NUMBER_LINES_ATTR = "lines"
        private const val TEXT_NUMBER_LINES_VALUE_MANY = -1
        private const val TEXT_NUMBER_LINES_VALUE_MANY_STRING = "many"
        private const val TEXT_NUMBER_LINES_VALUE_DEFAULT = 1

        /**
         * Applicable to type TEXT
         * Define if a text is a link
         * Boolean ("true" or "false")
         * "true" if not defined
         */
        private const val TEXT_LINK_ATTR = "link"
        private const val TEXT_LINK_VALUE_DEFAULT = false

        /**
         * Applicable to type TEXT
         * Define if a password generator is associated with the text
         * Boolean ("true" or "false")
         * "false" if not defined
         */
        private const val PASSWORD_GENERATOR_ATTR = "generator"
        private const val PASSWORD_GENERATOR_VALUE_DEFAULT = false

        /**
         * Applicable to type LIST
         * Define items of a list
         * List of items, separator is '|'
         */
        private const val LIST_ITEMS = "items"
        private const val LIST_ITEMS_SEPARATOR = "|"

        /**
         * Applicable to type DATETIME
         * Define the type of date
         * String ("date" or "time" or "datetime" or based on https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
         * "datetime" if not defined
         */
        private const val DATETIME_FORMAT_ATTR = "format"
        private const val DATETIME_FORMAT_VALUE_DATE = "date"
        private const val DATETIME_FORMAT_VALUE_TIME = "time"

        private fun removeSpecialChars(string: String): String {
            return string.filterNot { "{,:}".indexOf(it) > -1 }
        }

        fun getOptionsFromString(label: String): TemplateAttributeOption {
            val options = TemplateAttributeOption()
            val optionsMap =  if (label.contains("{") || label.contains("}")) {
                try {
                    label.trim().substringAfter("{").substringBefore("}")
                        .split(",").associate {
                            val keyValue = it.trim()
                            val (left, right) = keyValue.split(":")
                            left to right
                        }.toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } else {
                mutableMapOf()
            }
            options.mOptions.apply {
                clear()
                putAll(optionsMap)
            }
            return options
        }

        fun getStringFromOptions(options: TemplateAttributeOption): String {
            var optionsString = ""
            if (options.mOptions.isNotEmpty()) {
                optionsString += " {"
                var first = true
                for ((key, value) in options.mOptions) {
                    if (!first)
                        optionsString += ", "
                    first = false
                    optionsString += "${removeSpecialChars(key)}:${removeSpecialChars(value)}"
                }
                optionsString += "}"
            }
            return optionsString
        }
    }
}