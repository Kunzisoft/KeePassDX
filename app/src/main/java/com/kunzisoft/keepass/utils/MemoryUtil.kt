/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.utils

import android.os.Parcel
import android.os.Parcelable
import android.util.Log

import com.kunzisoft.keepass.stream.ActionReadBytes

import org.apache.commons.io.IOUtils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.HashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object MemoryUtil {

    private val TAG = MemoryUtil::class.java.name
    const val BUFFER_SIZE_BYTES = 3 * 128

    @Throws(IOException::class)
    fun copyStream(inputStream: InputStream, out: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        try {
            var read = inputStream.read(buffer)
            while (read != -1) {
                out.write(buffer, 0, read)
                read = inputStream.read(buffer)
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }
            }
        } catch (error: OutOfMemoryError) {
            throw IOException(error)
        }
    }

    @Throws(IOException::class)
    fun readBytes(inputStream: InputStream, actionReadBytes: ActionReadBytes) {
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var read = 0
        while (read != -1) {
            read = inputStream.read(buffer, 0, buffer.size)
            if (read != -1) {
                val optimizedBuffer: ByteArray = if (buffer.size == read) {
                    buffer
                } else {
                    buffer.copyOf(read)
                }
                actionReadBytes.doAction(optimizedBuffer)
            }
        }
    }

    @Throws(IOException::class)
    fun decompress(input: ByteArray): ByteArray {
        val bais = ByteArrayInputStream(input)
        val gzis = GZIPInputStream(bais)

        val baos = ByteArrayOutputStream()
        copyStream(gzis, baos)

        return baos.toByteArray()
    }

    @Throws(IOException::class)
    fun compress(input: ByteArray): ByteArray {
        val bais = ByteArrayInputStream(input)

        val baos = ByteArrayOutputStream()
        val gzos = GZIPOutputStream(baos)
        copyStream(bais, gzos)
        gzos.close()

        return baos.toByteArray()
    }

    /**
     * Compresses the input data using GZip and outputs the compressed data.
     *
     * @param input
     * An [InputStream] containing the input raw data.
     *
     * @return An [InputStream] to the compressed data.
     */
    fun compress(input: InputStream): InputStream {
        val compressedDataStream = PipedInputStream(3 * 128)
        Log.d(TAG, "About to compress input data using gzip asynchronously...")
        val compressionOutput: PipedOutputStream
        var gzipCompressedDataStream: GZIPOutputStream? = null
        try {
            compressionOutput = PipedOutputStream(compressedDataStream)
            gzipCompressedDataStream = GZIPOutputStream(compressionOutput)
            IOUtils.copy(input, gzipCompressedDataStream)
            Log.e(TAG, "Successfully compressed input data using gzip.")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to compress input data.", e)
        } finally {
            if (gzipCompressedDataStream != null) {
                try {
                    gzipCompressedDataStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to close gzip output stream.", e)
                }

            }
        }
        return compressedDataStream
    }

    // For writing to a Parcel
    fun <K : Parcelable, V : Parcelable> writeParcelableMap(
            parcel: Parcel, flags: Int, map: Map<K, V>) {
        parcel.writeInt(map.size)
        for ((key, value) in map) {
            parcel.writeParcelable(key, flags)
            parcel.writeParcelable(value, flags)
        }
    }

    // For reading from a Parcel
    fun <K : Parcelable, V : Parcelable> readParcelableMap(
            parcel: Parcel, kClass: Class<K>, vClass: Class<V>): Map<K, V> {
        val size = parcel.readInt()
        val map = HashMap<K, V>(size)
        for (i in 0 until size) {
            val key: K? = kClass.cast(parcel.readParcelable(kClass.classLoader))
            val value: V? = vClass.cast(parcel.readParcelable(vClass.classLoader))
            if (key != null && value != null)
                map[key] = value
        }
        return map
    }

    // For writing map with string key to a Parcel
    fun <V : Parcelable> writeStringParcelableMap(
            parcel: Parcel, flags: Int, map: Map<String, V>) {
        parcel.writeInt(map.size)
        for ((key, value) in map) {
            parcel.writeString(key)
            parcel.writeParcelable(value, flags)
        }
    }

    // For reading map with string key from a Parcel
    fun <V : Parcelable> readStringParcelableMap(
            parcel: Parcel, vClass: Class<V>): HashMap<String, V> {
        val size = parcel.readInt()
        val map = HashMap<String, V>(size)
        for (i in 0 until size) {
            val key: String? = parcel.readString()
            val value: V? = vClass.cast(parcel.readParcelable(vClass.classLoader))
            if (key != null && value != null)
                map[key] = value
        }
        return map
    }


    // For writing map with string key and string value to a Parcel
    fun writeStringParcelableMap(dest: Parcel, map: Map<String, String>) {
        dest.writeInt(map.size)
        for ((key, value) in map) {
            dest.writeString(key)
            dest.writeString(value)
        }
    }

    // For reading map with string key and string value from a Parcel
    fun readStringParcelableMap(parcel: Parcel): HashMap<String, String> {
        val size = parcel.readInt()
        val map = HashMap<String, String>(size)
        for (i in 0 until size) {
            val key: String? = parcel.readString()
            val value: String? = parcel.readString()
            if (key != null && value != null)
                map[key] = value
        }
        return map
    }
}
