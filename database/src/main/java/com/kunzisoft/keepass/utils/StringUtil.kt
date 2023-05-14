package com.kunzisoft.keepass.utils


object StringUtil {

    fun String.removeLineChars(): String {
        return this.replace("[\\r|\\n|\\t|\\u00A0]+".toRegex(), "")
    }

    fun String.removeSpaceChars(): String {
        return this.replace("[\\r|\\n|\\t|\\s|\\u00A0]+".toRegex(), "")
    }

    fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }

    fun Int.toFormattedColorString(showAlpha: Boolean = false): String {
        return if (showAlpha)
            String.format("#%08X", this)
        else
            String.format("#%06X", 16777215 and this)
    }

    fun String.toFormattedColorInt(): Int {
        if (this[0] == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            var color = this.substring(1).toLong(16)
            if (this.length == 7) {
                // Set the alpha value
                color = color or 0x00000000ff000000L
            } else require(this.length == 9) {"Unknown color" }
            return color.toInt()
        }
        throw IllegalArgumentException("Unknown color")
    }

}