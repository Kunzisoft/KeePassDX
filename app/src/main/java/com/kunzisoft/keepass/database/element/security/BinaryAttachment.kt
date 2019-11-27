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
import com.kunzisoft.keepass.database.element.PwDatabaseV4.Companion.BUFFER_SIZE_BYTES
import com.kunzisoft.keepass.stream.ReadBytes
import com.kunzisoft.keepass.stream.readFromStream
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BinaryAttachment : Parcelable {

    var isCompressed: Boolean? = null
        private set
    var isProtected: Boolean = false
        private set
    private var dataFile: File? = null

    fun length(): Long {
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
        this.dataFile = null
    }

    constructor(dataFile: File, enableProtection: Boolean = false, compressed: Boolean? = null) {
        this.isCompressed = compressed
        this.isProtected = enableProtection
        this.dataFile = dataFile
    }

    private constructor(parcel: Parcel) {
        val compressedByte = parcel.readByte().toInt()
        isCompressed = if (compressedByte == 2) null else compressedByte != 0
        isProtected = parcel.readByte().toInt() != 0
        dataFile = File(parcel.readString())
    }

    @Throws(IOException::class)
    fun getInputDataStream(): InputStream {
        return when {
            dataFile != null -> FileInputStream(dataFile!!)
            // TODO
            // else -> throw IOException("Unable to get binary data")
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    fun compress() {
        if (dataFile != null) {
            // To compress, create a new binary with file
            if (isCompressed != true) {
                val fileBinaryCompress = File(dataFile!!.parent, dataFile!!.name + "_temp")
                val outputStream = GZIPOutputStream(FileOutputStream(fileBinaryCompress))
                readFromStream(getInputDataStream(), BUFFER_SIZE_BYTES,
                        object : ReadBytes {
                            override fun read(buffer: ByteArray) {
                                outputStream.write(buffer)
                            }
                        })
                outputStream.close()

                // Remove unGzip file
                if (dataFile!!.delete()) {
                    if (fileBinaryCompress.renameTo(dataFile)) {
                        // Harmonize with database compression
                        isCompressed = true
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun decompress() {
        if (dataFile != null) {
            if (isCompressed != false) {
                val fileBinaryDecompress = File(dataFile!!.parent, dataFile!!.name + "_temp")
                val outputStream = FileOutputStream(fileBinaryDecompress)
                readFromStream(GZIPInputStream(getInputDataStream()), BUFFER_SIZE_BYTES,
                        object : ReadBytes {
                            override fun read(buffer: ByteArray) {
                                outputStream.write(buffer)
                            }
                        })
                outputStream.close()

                // Remove gzip file
                if (dataFile!!.delete()) {
                    if (fileBinaryDecompress.renameTo(dataFile)) {
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
                && sameData
    }

    override fun hashCode(): Int {

        var result = 0
        result = 31 * result + if (isCompressed == null) 2 else if (isCompressed!!) 1 else 0
        result = 31 * result + if (isProtected) 1 else 0
        result = 31 * result + dataFile!!.hashCode()
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompressed == null) 2 else if (isCompressed!!) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeString(dataFile?.absolutePath)
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
