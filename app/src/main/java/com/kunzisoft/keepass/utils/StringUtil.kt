package com.kunzisoft.keepass.utils

import java.text.Normalizer

object StringUtil {

    fun String.removeLineChars(): String {
        return this.replace("[\\r|\\n|\\t|\\u00A0]+".toRegex(), "")
    }

    fun String.removeSpaceChars(): String {
        return this.replace("[\\r|\\n|\\t|\\s|\\u00A0]+".toRegex(), "")
    }

    fun String.removeAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
    }

    fun ByteArray.toHexString() = joinToString("") { "%02X".format(it) }
}