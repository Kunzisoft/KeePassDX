/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.security

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import java.io.*

import java.util.Arrays

class ProtectedBinary : Parcelable {

    var isCompressed: Boolean? = null // Only for KDBX3.1-
    var isProtected: Boolean = false
    private var data: ByteArray? = null
    private var dataFile: File? = null

    fun length(): Long {
        if (data != null)
            return data!!.size.toLong()
        if (dataFile != null)
            return dataFile!!.length()
        return 0
    }

    /**
     * Empty protected binary
     */
    constructor() {
        this.isCompressed = null
        this.isProtected = false
        this.data = null
        this.dataFile = null
    }

    constructor(protectedBinary: ProtectedBinary) {
        this.isCompressed = protectedBinary.isCompressed
        this.isProtected = protectedBinary.isProtected
        this.data = protectedBinary.data
        this.dataFile = protectedBinary.dataFile
    }

    constructor(data: ByteArray?, enableProtection: Boolean = false, compressed: Boolean? = null) {
        this.isCompressed = compressed
        this.isProtected = enableProtection
        this.data = data
        this.dataFile = null
    }

    constructor(dataFile: File, enableProtection: Boolean = false, compressed: Boolean? = null) {
        this.isCompressed = compressed
        this.isProtected = enableProtection
        this.data = null
        this.dataFile = dataFile
    }

    private constructor(parcel: Parcel) {
        val compressedByte = parcel.readByte().toInt()
        isCompressed = if (compressedByte == 2) null else compressedByte != 0
        isProtected = parcel.readByte().toInt() != 0
        data = ByteArray(parcel.readInt())
        parcel.readByteArray(data)
        dataFile = File(parcel.readString())
    }

    @Throws(IOException::class)
    fun getInputDataStream(): InputStream {
        return when {
            data != null -> ByteArrayInputStream(data)
            dataFile != null -> FileInputStream(dataFile!!)
            else -> throw IOException("Unable to get binary data")
        }
    }

    fun clear() {
        data = null
        if (dataFile != null && !dataFile!!.delete())
            Log.e(TAG, "Unable to delete temp file " + dataFile!!.absolutePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null || javaClass != other.javaClass)
            return false
        if (other !is ProtectedBinary)
            return false

        var sameData = false
        if (data != null && Arrays.equals(data, other.data))
            sameData = true
        else if (dataFile != null && dataFile == other.dataFile)
            sameData = true
        else if (data == null && other.data == null
                && dataFile == null && other.dataFile == null)
            sameData = true

        return isCompressed == other.isCompressed
                && isProtected == other.isProtected
                && sameData
    }

    override fun hashCode(): Int {

        var result = 0
        result = 31 * result + if (isCompressed == null) 2 else if (isCompressed!!) 1 else 0
        result = 31 * result + if (isProtected) 1 else 0
        result = 31 * result + dataFile!!.hashCode()
        result = 31 * result + Arrays.hashCode(data)
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompressed == null) 2 else if (isCompressed!!) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeInt(data?.size ?: 0)
        dest.writeByteArray(data)
        dest.writeString(dataFile?.absolutePath)
    }

    companion object {

        private val TAG = ProtectedBinary::class.java.name

        @JvmField
        val CREATOR: Parcelable.Creator<ProtectedBinary> = object : Parcelable.Creator<ProtectedBinary> {
            override fun createFromParcel(parcel: Parcel): ProtectedBinary {
                return ProtectedBinary(parcel)
            }

            override fun newArray(size: Int): Array<ProtectedBinary?> {
                return arrayOfNulls(size)
            }
        }
    }

}
