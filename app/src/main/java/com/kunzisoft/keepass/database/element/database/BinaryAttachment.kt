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
import com.kunzisoft.keepass.database.element.Database
import org.apache.commons.io.output.CountingOutputStream
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec

class BinaryAttachment : Parcelable {

    private var dataFile: File? = null
    var length: Long = 0
        private set
    var isCompressed: Boolean = false
        private set
    var isProtected: Boolean = false
        private set
    var isCorrupted: Boolean = false
    // Cipher to encrypt temp file
    private var cipherEncryption: Cipher = Cipher.getInstance(Database.LoadedKey.BINARY_CIPHER)
    private var cipherDecryption: Cipher = Cipher.getInstance(Database.LoadedKey.BINARY_CIPHER)

    /**
     * Empty protected binary
     */
    constructor()

    constructor(dataFile: File, compressed: Boolean = false, protected: Boolean = false) {
        this.dataFile = dataFile
        this.length = 0
        this.isCompressed = compressed
        this.isProtected = protected
    }

    private constructor(parcel: Parcel) {
        parcel.readString()?.let {
            dataFile = File(it)
        }
        length = parcel.readLong()
        isCompressed = parcel.readByte().toInt() != 0
        isProtected = parcel.readByte().toInt() != 0
        isCorrupted = parcel.readByte().toInt() != 0
    }

    @Throws(IOException::class)
    fun getInputDataStream(cipherKey: Database.LoadedKey): InputStream {
        return buildInputStream(dataFile!!, cipherKey)
    }

    @Throws(IOException::class)
    fun getOutputDataStream(cipherKey: Database.LoadedKey): OutputStream {
        return buildOutputStream(dataFile!!, cipherKey)
    }

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
    private fun buildInputStream(file: File?, cipherKey: Database.LoadedKey): InputStream {
        return when {
            file != null && file.length() > 0 -> {
                cipherDecryption.init(Cipher.DECRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
                Base64InputStream(CipherInputStream(FileInputStream(file), cipherDecryption), Base64.NO_WRAP)
            }
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    private fun buildOutputStream(file: File?, cipherKey: Database.LoadedKey): OutputStream {
        return when {
            file != null -> {
                cipherEncryption.init(Cipher.ENCRYPT_MODE, cipherKey.key, IvParameterSpec(cipherKey.iv))
                BinaryCountingOutputStream(Base64OutputStream(CipherOutputStream(FileOutputStream(file), cipherEncryption), Base64.NO_WRAP))
            }
            else -> throw IOException("Unable to write in an unknown file")
        }
    }

    @Throws(IOException::class)
    fun compress(cipherKey: Database.LoadedKey, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            // To compress, create a new binary with file
            if (!isCompressed) {
                // Encrypt the new gzipped temp file
                val fileBinaryCompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                getInputDataStream(cipherKey).use { inputStream ->
                    GZIPOutputStream(buildOutputStream(fileBinaryCompress, cipherKey)).use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize)
                    }
                }
                // Remove ungzip file
                if (concreteDataFile.delete()) {
                    if (fileBinaryCompress.renameTo(concreteDataFile)) {
                        // Harmonize with database compression
                        isCompressed = true
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun decompress(cipherKey: Database.LoadedKey, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            if (isCompressed) {
                // Encrypt the new ungzipped temp file
                val fileBinaryDecompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                getUnGzipInputDataStream(cipherKey).use { inputStream ->
                    buildOutputStream(fileBinaryDecompress, cipherKey).use { outputStream ->
                        inputStream.copyTo(outputStream, bufferSize)
                    }
                }
                // Remove gzip file
                if (concreteDataFile.delete()) {
                    if (fileBinaryDecompress.renameTo(concreteDataFile)) {
                        // Harmonize with database compression
                        isCompressed = false
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun clear() {
        if (dataFile != null && !dataFile!!.delete())
            throw IOException("Unable to delete temp file " + dataFile!!.absolutePath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null || javaClass != other.javaClass)
            return false
        if (other !is BinaryAttachment)
            return false

        var sameData = false
        if (dataFile != null && dataFile == other.dataFile)
            sameData = true

        return isCompressed == other.isCompressed
                && isProtected == other.isProtected
                && isCorrupted == other.isCorrupted
                && sameData
    }

    override fun hashCode(): Int {

        var result = 0
        result = 31 * result + if (isCompressed) 1 else 0
        result = 31 * result + if (isProtected) 1 else 0
        result = 31 * result + if (isCorrupted) 1 else 0
        result = 31 * result + dataFile!!.hashCode()
        result = 31 * result + length.hashCode()
        return result
    }

    override fun toString(): String {
        return dataFile.toString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(dataFile?.absolutePath)
        dest.writeLong(length)
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByte((if (isCorrupted) 1 else 0).toByte())
    }

    /**
     * Custom OutputStream to calculate the size of binary file
     */
    private inner class BinaryCountingOutputStream(out: OutputStream): CountingOutputStream(out) {
        init {
            length = 0
        }

        override fun beforeWrite(n: Int) {
            super.beforeWrite(n)
            length = byteCount
        }

        override fun close() {
            super.close()
            length = byteCount
        }
    }

    companion object {

        private val TAG = BinaryAttachment::class.java.name

        @JvmField
        val CREATOR: Parcelable.Creator<BinaryAttachment> = object : Parcelable.Creator<BinaryAttachment> {
            override fun createFromParcel(parcel: Parcel): BinaryAttachment {
                return BinaryAttachment(parcel)
            }

            override fun newArray(size: Int): Array<BinaryAttachment?> {
                return arrayOfNulls(size)
            }
        }
    }

}
