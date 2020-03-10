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
package com.kunzisoft.keepass.database.element.security

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.stream.readBytes
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
    fun compress(bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            // To compress, create a new binary with file
            if (isCompressed != true) {
                val fileBinaryCompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                var outputStream: GZIPOutputStream? = null
                var inputStream: InputStream? = null
                try {
                    outputStream = GZIPOutputStream(FileOutputStream(fileBinaryCompress))
                    inputStream = getInputDataStream()
                    inputStream.readBytes(bufferSize) { buffer ->
                        outputStream.write(buffer)
                    }
                } finally {
                    inputStream?.close()
                    outputStream?.close()

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
    }

    @Throws(IOException::class)
    fun decompress(bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        dataFile?.let { concreteDataFile ->
            if (isCompressed != false) {
                val fileBinaryDecompress = File(concreteDataFile.parent, concreteDataFile.name + "_temp")
                var outputStream: FileOutputStream? = null
                var inputStream: GZIPInputStream? = null
                try {
                    outputStream = FileOutputStream(fileBinaryDecompress)
                    inputStream = GZIPInputStream(getInputDataStream())
                    inputStream.readBytes(bufferSize) { buffer ->
                        outputStream.write(buffer)
                    }
                } finally {
                    inputStream?.close()
                    outputStream?.close()

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
    }

    fun download(createdFileUri: Uri,
                 contentResolver: ContentResolver,
                 bufferSize: Int = DEFAULT_BUFFER_SIZE,
                 update: ((percent: Int)->Unit)? = null) {

        var dataDownloaded = 0
        contentResolver.openOutputStream(createdFileUri).use { outputStream ->
            outputStream?.let { fileOutputStream ->
                if (isCompressed == true) {
                    GZIPInputStream(getInputDataStream())
                } else {
                    getInputDataStream()
                }.use { inputStream ->
                    inputStream.readBytes(bufferSize) { buffer ->
                        fileOutputStream.write(buffer)
                        dataDownloaded += buffer.size
                        try {
                            val percentDownload = (100 * dataDownloaded / length()).toInt()
                            update?.invoke(percentDownload)
                        } catch (e: Exception) {}
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
