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
package com.kunzisoft.keepass.database.element.binary

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64InputStream
import android.util.Base64OutputStream
import com.kunzisoft.keepass.database.element.binary.BinaryCache.Companion.UNKNOWN
import com.kunzisoft.keepass.utils.readAllBytes
import java.io.*
import java.util.zip.GZIPOutputStream

class BinaryByte : BinaryData {

    private var mDataByteId: String

    private fun getByteArray(binaryCache: BinaryCache): ByteArray {
        val keyData = binaryCache.getByteArray(mDataByteId)
        mDataByteId = keyData.key
        return keyData.data
    }

    constructor() : super() {
        mDataByteId = UNKNOWN
    }

    constructor(id: String,
                compressed: Boolean = false,
                protected: Boolean = false) : super(compressed, protected) {
        mDataByteId = id
    }

    constructor(parcel: Parcel) : super(parcel) {
        mDataByteId = parcel.readString() ?: UNKNOWN
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(mDataByteId)
    }

    @Throws(IOException::class)
    override fun getInputDataStream(binaryCache: BinaryCache): InputStream {
        return Base64InputStream(ByteArrayInputStream(getByteArray(binaryCache)), BASE64_FLAG)
    }

    @Throws(IOException::class)
    override fun getOutputDataStream(binaryCache: BinaryCache): OutputStream {
        return BinaryCountingOutputStream(Base64OutputStream(ByteOutputStream(binaryCache), BASE64_FLAG))
    }

    @Throws(IOException::class)
    override fun compress(binaryCache: BinaryCache) {
        if (!isCompressed) {
            GZIPOutputStream(getOutputDataStream(binaryCache)).use { outputStream ->
                getInputDataStream(binaryCache).use { inputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = true
            }
        }
    }

    @Throws(IOException::class)
    override fun decompress(binaryCache: BinaryCache) {
        if (isCompressed) {
            getUnGzipInputDataStream(binaryCache).use { inputStream ->
                getOutputDataStream(binaryCache).use { outputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = false
            }
        }
    }

    @Throws(IOException::class)
    override fun clear(binaryCache: BinaryCache) {
        binaryCache.removeByteArray(mDataByteId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryByte) return false
        if (!super.equals(other)) return false

        if (mDataByteId != other.mDataByteId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mDataByteId.hashCode()
        return result
    }

    /**
     * Custom OutputStream to calculate the size and hash of binary file
     */
    private inner class ByteOutputStream(private val binaryCache: BinaryCache) : ByteArrayOutputStream() {
        override fun close() {
            binaryCache.setByteArray(mDataByteId, this.toByteArray())
            super.close()
        }
    }

    companion object {
        private val TAG = BinaryByte::class.java.name

        @JvmField
        val CREATOR: Parcelable.Creator<BinaryByte> = object : Parcelable.Creator<BinaryByte> {
            override fun createFromParcel(parcel: Parcel): BinaryByte {
                return BinaryByte(parcel)
            }

            override fun newArray(size: Int): Array<BinaryByte?> {
                return arrayOfNulls(size)
            }
        }
    }

}
