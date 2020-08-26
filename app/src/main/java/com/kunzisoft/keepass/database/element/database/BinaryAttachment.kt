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
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class BinaryAttachment : Parcelable {

    var isCompressed: Boolean = false
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
        this.isCompressed = false
        this.isProtected = false
        this.dataFile = null
    }

    constructor(dataFile: File, enableProtection: Boolean = false, compressed: Boolean = false) {
        this.isCompressed = compressed
        this.isProtected = enableProtection
        this.dataFile = dataFile
    }

    private constructor(parcel: Parcel) {
        val compressedByte = parcel.readByte().toInt()
        isCompressed = compressedByte != 0
        isProtected = parcel.readByte().toInt() != 0
        parcel.readString()?.let {
            dataFile = File(it)
        }
    }

    @Throws(IOException::class)
    fun getInputDataStream(): InputStream {
        return when {
            dataFile != null -> FileInputStream(dataFile!!)
            else -> ByteArrayInputStream(ByteArray(0))
        }
    }

    @Throws(IOException::class)
    fun getOutputDataStream(): OutputStream {
        return when {
            dataFile != null -> FileOutputStream(dataFile!!)
            else -> throw IOException("Unable to write in an unknown file")
        }
    }

    @Throws(IOException::class)
    fun compress(bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            // To compress, create a new binary with file
            if (!isCompressed) {
                val fileBinaryCompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                GZIPOutputStream(FileOutputStream(fileBinaryCompress)).use { outputStream ->
                    getInputDataStream().use { inputStream ->
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
            } else {
                isCompressed = true
            }
        }
    }

    @Throws(IOException::class)
    fun decompress(bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            if (isCompressed) {
                val fileBinaryDecompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                FileOutputStream(fileBinaryDecompress).use { outputStream ->
                    GZIPInputStream(getInputDataStream()).use { inputStream ->
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
            } else {
                isCompressed = false
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
        result = 31 * result + if (isCompressed) 1 else 0
        result = 31 * result + if (isProtected) 1 else 0
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
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
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
