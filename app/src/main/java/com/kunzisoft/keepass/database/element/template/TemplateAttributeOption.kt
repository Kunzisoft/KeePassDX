package com.kunzisoft.keepass.database.element.template

import com.kunzisoft.keepass.database.element.DateInstant

class TemplateAttributeOption {

    companion object {

        /**
         * Applicable to type TEXT
         * Integer, can be "-1" to infinite value
         * "1" if not defined
         */
        const val NUMBER_LINES = "lines"
        const val NUMBER_LINES_INFINITE = "-1"

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

        fun getNumberLines(options: LinkedHashMap<String, String>): Int {
            return try {
                options[NUMBER_LINES]?.toInt() ?: 1
            } catch (e: Exception) {
                1
            }
        }

        fun isLinkify(options: LinkedHashMap<String, String>): Boolean {
            return try {
                options[LINKIFY]?.toBoolean() ?: true
            } catch (e: Exception) {
                true
            }
        }

        fun getDateFormat(options: LinkedHashMap<String, String>): DateInstant.Type {
            return when (options[DATETIME_FORMAT]) {
                DATETIME_FORMAT_DATE -> DateInstant.Type.DATE
                DATETIME_FORMAT_TIME -> DateInstant.Type.TIME
                else -> DateInstant.Type.DATE_TIME
            }
        }

        private fun getOneOption(label: String): String? {
            return if (label.contains("{") && label.contains("}")) {
                label.substringAfter("{").substringBefore("}")
            } else {
                null
            }
        }

        fun getOptionsFromString(label: String): LinkedHashMap<String, String> {
            val options = LinkedHashMap<String, String>()
            if (label.contains("[") && label.contains("]")) {
                var newString = label.substringAfter("[").substringBefore("]")
                var option = getOneOption(newString)
                while (option != null && option.contains(":")) {
                    val optionList = option.split(":")
                    options[optionList[0]] = optionList[1]
                    newString = newString.substringAfter("}")
                    option = getOneOption(newString)
                }
            }
            return options
        }

        fun getStringFromOptions(options: LinkedHashMap<String, String>): String {
            var optionsString = ""
            if (options.isNotEmpty()) {
                optionsString += "["
                for ((key, value) in options) {
                    optionsString += "{$key:$value}"
                }
                optionsString += "]"
            }
            return optionsString
        }
    }
}