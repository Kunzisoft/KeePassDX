package com.kunzisoft.keepass.utils

import java.text.Normalizer

object StringUtil {

    fun String.removeLineChars(): String {
        return this.replace("[\\r|\\n|\\t|\\u00A0]+".toRegex(), "")
    }

    fun String.removeSpaceChars(): String {
        return this.replace("[\\r|\\n|\\t|\\s|\\u00A0]+".toRegex(), "")
    }

    fun String.flattenToAscii(): String {
        var string = this
        val out = CharArray(string.length)
        string = Normalizer.normalize(string, Normalizer.Form.NFD)
        var j = 0
        var i = 0
        val n = string.length
        while (i < n) {
            val c = string[i]
            if (c <= '\u007F') out[j++] = c
            ++i
        }
        return String(out)
    }

    fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }
}