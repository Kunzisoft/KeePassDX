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
import com.kunzisoft.keepass.stream.readBytes
import java.io.*
import java.security.Key
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

class BinaryAttachment : Parcelable {

    private var dataFile: File? = null
    var isCompressed: Boolean = false
        private set
    var isProtected: Boolean = false
        private set
    var isCorrupted: Boolean = false
    // Cipher to encrypt temp file
    private var cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

    fun length(): Long {
        return dataFile?.length() ?: 0
    }

    /**
     * Empty protected binary
     */
    constructor()

    constructor(dataFile: File, compressed: Boolean = false, protected: Boolean = false) {
        this.dataFile = dataFile
        this.isCompressed = compressed
        this.isProtected = protected
    }

    private constructor(parcel: Parcel) {
        parcel.readString()?.let {
            dataFile = File(it)
        }
        isCompressed = parcel.readByte().toInt() != 0
        isProtected = parcel.readByte().toInt() != 0
        isCorrupted = parcel.readByte().toInt() != 0
    }

    @Throws(IOException::class)
    fun getInputDataStream(cipherKey: Key): InputStream {
        return when {
            length() > 0 -> {
                cipher.init(Cipher.DECRYPT_MODE, cipherKey)
                CipherInputStream(FileInputStream(dataFile!!), cipher)
            }
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    fun getUnGzipInputDataStream(cipherKey: Key): InputStream {
        return if (isCompressed)
            GZIPInputStream(getInputDataStream(cipherKey))
        else
            getInputDataStream(cipherKey)
    }

    @Throws(IOException::class)
    fun getOutputDataStream(cipherKey: Key): OutputStream {
        return when {
            dataFile != null -> {
                cipher.init(Cipher.ENCRYPT_MODE, cipherKey)
                CipherOutputStream(FileOutputStream(dataFile!!), cipher)
            }
            else -> throw IOException("Unable to write in an unknown file")
        }
    }

    @Throws(IOException::class)
    fun getGzipOutputDataStream(cipherKey: Key): OutputStream {
        return if (isCompressed) {
            GZIPOutputStream(getOutputDataStream(cipherKey))
        } else {
            getOutputDataStream(cipherKey)
        }
    }

    @Throws(IOException::class)
    fun compress(cipherKey: Key, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            // To compress, create a new binary with file
            if (!isCompressed) {
                val fileBinaryCompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                GZIPOutputStream(FileOutputStream(fileBinaryCompress)).use { outputStream ->
                    getInputDataStream(cipherKey).use { inputStream ->
                        inputStream.readBytes(bufferSize) { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }
                // Remove unGzip file
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
    fun decompress(cipherKey: Key, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            if (isCompressed) {
                val fileBinaryDecompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                FileOutputStream(fileBinaryDecompress).use { outputStream ->
                    getUnGzipInputDataStream(cipherKey).use { inputStream ->
                        inputStream.readBytes(bufferSize) { buffer ->
                            outputStream.write(buffer)
                        }
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
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByte((if (isCorrupted) 1 else 0).toByte())
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
