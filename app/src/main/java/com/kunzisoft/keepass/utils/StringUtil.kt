package com.kunzisoft.keepass.utils

object StringUtil {

    fun String.removeLineChars(): String {
        return this.replace("[\\r|\\n|\\t|\\u00A0]+".toRegex(), "")
    }

    fun String.removeSpaceChars(): String {
        return this.replace("[\\r|\\n|\\t|\\s|\\u00A0]+".toRegex(), "")
    }

    fun String.hexStringToByteArray(): ByteArray {
        val len = this.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4)
                    + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }
}