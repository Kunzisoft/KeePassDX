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

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Arrays

class ProtectedBinary : Parcelable {

    var isProtected: Boolean = false
        private set
    private var data: ByteArray? = null
    private var dataFile: File? = null
    private var size: Int = 0

    fun length(): Long {
        if (data != null)
            return data!!.size.toLong()
        return if (dataFile != null) size.toLong() else 0
    }

    /**
     * Empty protected binary
     */
    constructor() {
        this.isProtected = false
        this.data = null
        this.dataFile = null
        this.size = 0
    }

    constructor(protectedBinary: ProtectedBinary) {
        this.isProtected = protectedBinary.isProtected
        this.data = protectedBinary.data
        this.dataFile = protectedBinary.dataFile
        this.size = protectedBinary.size
    }

    constructor(enableProtection: Boolean, data: ByteArray?) {
        this.isProtected = enableProtection
        this.data = data
        this.dataFile = null
        if (data != null)
            this.size = data.size
        else
            this.size = 0
    }

    constructor(enableProtection: Boolean, dataFile: File, size: Int) {
        this.isProtected = enableProtection
        this.data = null
        this.dataFile = dataFile
        this.size = size
    }

    private constructor(parcel: Parcel) {
        isProtected = parcel.readByte().toInt() != 0
        parcel.readByteArray(data)
        dataFile = File(parcel.readString())
        size = parcel.readInt()
    }

    @Throws(IOException::class)
    fun getData(): InputStream? {
        return when {
            data != null -> ByteArrayInputStream(data)
            dataFile != null -> FileInputStream(dataFile!!)
            else -> null
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
        return isProtected == other.isProtected &&
                size == other.size &&
                Arrays.equals(data, other.data) &&
                dataFile != null &&
                dataFile == other.dataFile
    }

    override fun hashCode(): Int {

        var result = 0
        result = 31 * result + if (isProtected) 1 else 0
        result = 31 * result + dataFile!!.hashCode()
        result = 31 * result + Integer.valueOf(size)!!.hashCode()
        result = 31 * result + Arrays.hashCode(data)
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByteArray(data)
        dest.writeString(dataFile!!.absolutePath)
        dest.writeInt(size)
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
