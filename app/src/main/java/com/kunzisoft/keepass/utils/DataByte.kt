package com.kunzisoft.keepass.utils

import android.content.Context
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

class DataByte(var number: Long, var format: ByteFormat) {

    fun toBetterByteFormat(): DataByte {
        return when (this.format) {
            ByteFormat.BYTE -> {
                if (this.number % MEBIBYTES == 0L) {
                    DataByte((this.number / MEBIBYTES), ByteFormat.MEBIBYTE)
                } else {
                    DataByte(this.number, ByteFormat.BYTE)
                }
            }
            ByteFormat.MEBIBYTE -> this
        }
    }

    /**
     * Number of bytes in current DataByte
     */
    fun toBytes(): Long {
        return when (this.format) {
            ByteFormat.BYTE -> this.number
            ByteFormat.MEBIBYTE -> this.number * MEBIBYTES
        }
    }

    override fun toString(): String {
        return "$number ${format.name}"
    }

    fun toString(context: Context): String {
        return "$number ${context.getString(format.stringId)}"
    }

    enum class ByteFormat(@StringRes var stringId: Int) {
        BYTE(R.string.unit_byte),
        MEBIBYTE(R.string.unit_mebibyte)
    }

    companion object {
        const val MEBIBYTES = 1048576L
    }
}