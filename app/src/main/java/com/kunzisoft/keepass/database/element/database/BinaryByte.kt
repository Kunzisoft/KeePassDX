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
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.stream.readAllBytes
import java.io.*
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec

class BinaryByte : BinaryData {

    private var mDataByteId: Int? = null

    private fun getByteArray(): ByteArray {
        val keyData = App.getByteArray(mDataByteId)
        mDataByteId = keyData.key
        return keyData.data
    }

    /**
     * Empty protected binary
     */
    constructor() : super() {
        getByteArray()
    }

    constructor(compressed: Boolean = false,
                protected: Boolean = false) : super(compressed, protected) {
        getByteArray()
    }

    constructor(mDataByteId: Int,
                compressed: Boolean = false,
                protected: Boolean = false) : super(compressed, protected) {
        this.mDataByteId = mDataByteId
    }

    constructor(parcel: Parcel) : super(parcel) {
        mDataByteId = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        mDataByteId?.let {
            dest.writeInt(it)
        }
    }

    @Throws(IOException::class)
    override fun getInputDataStream(cipherKey: Database.LoadedKey): InputStream {
        return when {
            getSize() > 0 -> {
                cipherDecryption.init(Cipher.DECRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
                Base64InputStream(CipherInputStream(ByteArrayInputStream(getByteArray()), cipherDecryption), Base64.NO_WRAP)
            }
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    override fun getOutputDataStream(cipherKey: Database.LoadedKey): OutputStream {
        cipherEncryption.init(Cipher.ENCRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
        return BinaryCountingOutputStream(Base64OutputStream(CipherOutputStream(ByteOutputStream(), cipherEncryption), Base64.NO_WRAP))
    }

    @Throws(IOException::class)
    override fun compress(cipherKey: Database.LoadedKey) {
        if (!isCompressed) {
            GZIPOutputStream(getOutputDataStream(cipherKey)).use { outputStream ->
                getInputDataStream(cipherKey).use { inputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = true
            }
        }
    }

    @Throws(IOException::class)
    override fun decompress(cipherKey: Database.LoadedKey) {
        if (isCompressed) {
            getUnGzipInputDataStream(cipherKey).use { inputStream ->
                getOutputDataStream(cipherKey).use { outputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = false
            }
        }
    }

    @Throws(IOException::class)
    override fun clear() {
        App.removeByteArray(mDataByteId)
    }

    override fun toString(): String {
        return getByteArray().toString()
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
        result = 31 * result + (mDataByteId ?: 0)
        return result
    }

    /**
     * Custom OutputStream to calculate the size and hash of binary file
     */
    private inner class ByteOutputStream : ByteArrayOutputStream() {
        override fun close() {
            App.setByteArray(mDataByteId, this.toByteArray())
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
