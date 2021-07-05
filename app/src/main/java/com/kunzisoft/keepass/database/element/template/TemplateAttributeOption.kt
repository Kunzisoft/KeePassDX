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
        const val NUMBER_LINES = "lines"
        const val NUMBER_LINES_MANY = "many"

        /**
         * Applicable to type TEXT
         * Boolean ("true" or "false")
         * "true" if not defined
          */
        const val LINKIFY = "linkify"

        /**
         * Applicable to type DATETIME
         * String ("date" or "time" or "datetime" or based on https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html)
         * "datetime" if not defined
         */
        const val DATETIME_FORMAT = "format"
        const val DATETIME_FORMAT_DATE = "date"
        const val DATETIME_FORMAT_TIME = "time"
        const val DATETIME_FORMAT_MONTH_YEAR = "MM yy"

        fun getNumberLines(options: MutableMap<String, String>): Int {
            return try {
                val value = options[NUMBER_LINES]
                if (value == NUMBER_LINES_MANY)
                    -1
                else
                    options[NUMBER_LINES]?.toInt() ?: 1
            } catch (e: Exception) {
                1
            }
        }

        fun isLinkify(options: MutableMap<String, String>): Boolean {
            return try {
                options[LINKIFY]?.toBoolean() ?: true
            } catch (e: Exception) {
                true
            }
        }

        fun getDateFormat(options: MutableMap<String, String>): DateInstant.Type {
            return when (options[DATETIME_FORMAT]) {
                DATETIME_FORMAT_DATE -> DateInstant.Type.DATE
                DATETIME_FORMAT_TIME -> DateInstant.Type.TIME
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
                }
                optionsString += "}"
            }
            return optionsString
        }
    }
}