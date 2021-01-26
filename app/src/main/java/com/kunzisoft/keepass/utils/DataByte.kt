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
 *
 */
package com.kunzisoft.keepass.utils

import android.content.Context
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

class DataByte(var number: Long, var format: ByteFormat) {

    fun toBetterByteFormat(): DataByte {
        return when (this.format) {
            ByteFormat.BYTE -> {
                when {
                    //this.number % GIBIBYTES == 0L -> {
                    //    DataByte((this.number / GIBIBYTES), ByteFormat.GIBIBYTE)
                    //}
                    this.number % MEBIBYTES == 0L -> {
                        DataByte((this.number / MEBIBYTES), ByteFormat.MEBIBYTE)
                    }
                    this.number % KIBIBYTES == 0L -> {
                        DataByte((this.number / KIBIBYTES), ByteFormat.KIBIBYTE)
                    }
                    else -> {
                        DataByte(this.number, ByteFormat.BYTE)
                    }
                }
            }
            else -> {
                DataByte(toBytes(), ByteFormat.BYTE).toBetterByteFormat()
            }
        }
    }

    /**
     * Number of bytes in current DataByte
     */
    fun toBytes(): Long {
        return when (this.format) {
            ByteFormat.BYTE -> this.number
            ByteFormat.KIBIBYTE -> this.number * KIBIBYTES
            ByteFormat.MEBIBYTE -> this.number * MEBIBYTES
            //ByteFormat.GIBIBYTE -> this.number * GIBIBYTES
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
        KIBIBYTE(R.string.unit_kibibyte),
        MEBIBYTE(R.string.unit_mebibyte)
        //GIBIBYTE(R.string.unit_gibibyte)
    }

    companion object {
        const val KIBIBYTES = 1024L
        const val MEBIBYTES = 1048576L
        const val GIBIBYTES = 1073741824L
    }
}