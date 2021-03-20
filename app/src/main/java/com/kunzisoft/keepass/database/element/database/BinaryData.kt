/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.database

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Database
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

abstract class BinaryData : Parcelable {

    var isCompressed: Boolean = false
        protected set
    var isProtected: Boolean = false
        protected set
    var isCorrupted: Boolean = false

    /**
     * Empty protected binary
     */
    protected constructor()

    protected constructor(compressed: Boolean = false, protected: Boolean = false) {
        this.isCompressed = compressed
        this.isProtected = protected
    }

    protected constructor(parcel: Parcel) {
        isCompressed = parcel.readByte().toInt() != 0
        isProtected = parcel.readByte().toInt() != 0
        isCorrupted = parcel.readByte().toInt() != 0
    }

    @Throws(IOException::class)
    abstract fun getInputDataStream(cipherKey: Database.LoadedKey): InputStream

    @Throws(IOException::class)
    abstract fun getOutputDataStream(cipherKey: Database.LoadedKey): OutputStream

    @Throws(IOException::class)
    fun getUnGzipInputDataStream(cipherKey: Database.LoadedKey): InputStream {
        return if (isCompressed) {
            GZIPInputStream(getInputDataStream(cipherKey))
        } else {
            getInputDataStream(cipherKey)
        }
    }

    @Throws(IOException::class)
    fun getGzipOutputDataStream(cipherKey: Database.LoadedKey): OutputStream {
        return if (isCompressed) {
            GZIPOutputStream(getOutputDataStream(cipherKey))
        } else {
            getOutputDataStream(cipherKey)
        }
    }

    @Throws(IOException::class)
    abstract fun compress(cipherKey: Database.LoadedKey)

    @Throws(IOException::class)
    abstract fun decompress(cipherKey: Database.LoadedKey)

    @Throws(IOException::class)
    abstract fun clear()

    abstract fun dataExists(): Boolean

    abstract fun getSize(): Long

    abstract fun binaryHash(): Int

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByte((if (isCorrupted) 1 else 0).toByte())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryData) return false

        if (isCompressed != other.isCompressed) return false
        if (isProtected != other.isProtected) return false
        if (isCorrupted != other.isCorrupted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isCompressed.hashCode()
        result = 31 * result + isProtected.hashCode()
        result = 31 * result + isCorrupted.hashCode()
        return result
    }

    companion object {
        private val TAG = BinaryData::class.java.name
    }

}
