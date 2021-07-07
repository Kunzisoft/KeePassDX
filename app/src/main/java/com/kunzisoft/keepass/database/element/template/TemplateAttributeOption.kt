package com.kunzisoft.keepass.database.element.template

import com.kunzisoft.keepass.database.element.DateInstant
import java.net.URLDecoder
import java.net.URLEncoder

class TemplateAttributeOption {

    companion object {

        /**
         * Applicable to type TEXT
         * Integer, can be "-1" or "many" to infinite value
         * "1" if not defined
         */
        const val TEXT_NUMBER_LINES_ATTR = "lines"
        const val TEXT_NUMBER_LINES_VALUE_MANY = "many"
        const val TEXT_NUMBER_LINES_VALUE_DEFAULT = "1"

        /**
         * Applicable to type TEXT
         * Integer, can be "-1" or "many" to infinite value
         * "1" if not defined
         */
        const val TEXT_NUMBER_CHARS_ATTR = "chars"
        const val TEXT_NUMBER_CHARS_VALUE_DEFAULT = "many"

        /**
         * Applicable to type TEXT
         * Boolean ("true" or "false")
         * "true" if not defined
          */
        const val TEXT_LINK_ATTR = "link"
        const val TEXT_LINK_VALUE_DEFAULT = "false"

        /**
         * Applicable to type LIST
         * List of items, separator is '|'
         */
        const val LIST_ITEMS = "items"

        /**
         * Applicable to type LIST
         * Default string element representation
         * 1st element if not defined
         */
        const val LIST_DEFAULT_ITEM_ATTR = "default"
        const val LIST_DEFAULT_ITEM_VALUE_DEFAULT = "1"

        /**
         * Applicable to type DATETIME
         * String ("date" or "time" or "datetime" or based on https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
         * "datetime" if not defined
         */
        const val DATETIME_FORMAT_ATTR = "format"
        const val DATETIME_FORMAT_VALUE_DATE = "date"
        const val DATETIME_FORMAT_VALUE_TIME = "time"
        const val DATETIME_FORMAT_VALUE_DEFAULT = "datetime"

        fun getNumberLines(options: MutableMap<String, String>): Int {
            return try {
                val value = options[TEXT_NUMBER_LINES_ATTR]
                if (value == TEXT_NUMBER_LINES_VALUE_MANY)
                    -1
                else
                    options[TEXT_NUMBER_LINES_ATTR]?.toInt() ?: 1
            } catch (e: Exception) {
                1
            }
        }

        fun isLinkify(options: MutableMap<String, String>): Boolean {
            return try {
                options[TEXT_LINK_ATTR]?.toBoolean() ?: true
            } catch (e: Exception) {
                true
            }
        }

        fun listItemsFromString(itemsString: String): List<String> {
            return itemsString.split("|")
        }

        fun stringFromListItems(items: List<String>): String {
            return items.joinToString("|")
        }

        fun getDateFormat(options: MutableMap<String, String>): DateInstant.Type {
            return when (options[DATETIME_FORMAT_ATTR]) {
                DATETIME_FORMAT_VALUE_DATE -> DateInstant.Type.DATE
                DATETIME_FORMAT_VALUE_TIME -> DateInstant.Type.TIME
                else -> DateInstant.Type.DATE_TIME
            }
        }

        fun getOptionsFromString(label: String): MutableMap<String, String> {
            return if (label.contains("{") || label.contains("}")) {
                try {
                    label.trim().substringAfter("{").substringBefore("}")
                        .split(",").associate {
                            val (left, right) = it.split(":")
                            URLDecoder.decode(left, "utf-8") to URLDecoder.decode(right, "utf-8")
                        }.toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            } else {
                mutableMapOf()
            }
        }

        fun getStringFromOptions(options: Map<String, String>): String {
            var optionsString = ""
            if (options.isNotEmpty()) {
                optionsString += " {"
                var first = true
                for ((key, value) in options) {
                    if (!first)
                        optionsString += ","
                    first = false
                    optionsString += "${URLEncoder.encode(key, "utf-8")}:${URLEncoder.encode(value, "utf-8")}"
                    // "|" is ok and part of a list
                    optionsString = optionsString.replace("%7C", "|")
                }
                optionsString += "}"
            }
            return optionsString
        }
    }
}