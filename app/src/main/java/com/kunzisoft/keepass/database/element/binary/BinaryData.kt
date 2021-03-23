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

import android.app.ActivityManager
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
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

    protected constructor(compressed: Boolean = false, protected: Boolean = false) {
        this.isCompressed = compressed
        this.isProtected = protected
    }

    protected constructor(parcel: Parcel) {
        isCompressed = parcel.readByte().toInt() != 0
        isProtected = parcel.readByte().toInt() != 0
        isCorrupted = parcel.readByte().toInt() != 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByte((if (isCorrupted) 1 else 0).toByte())
    }

    @Throws(IOException::class)
    abstract fun getInputDataStream(binaryCache: BinaryCache): InputStream

    @Throws(IOException::class)
    abstract fun getOutputDataStream(binaryCache: BinaryCache): OutputStream

    @Throws(IOException::class)
    fun getUnGzipInputDataStream(binaryCache: BinaryCache): InputStream {
        return if (isCompressed) {
            GZIPInputStream(getInputDataStream(binaryCache))
        } else {
            getInputDataStream(binaryCache)
        }
    }

    @Throws(IOException::class)
    fun getGzipOutputDataStream(binaryCache: BinaryCache): OutputStream {
        return if (isCompressed) {
            GZIPOutputStream(getOutputDataStream(binaryCache))
        } else {
            getOutputDataStream(binaryCache)
        }
    }

    @Throws(IOException::class)
    abstract fun compress(binaryCache: BinaryCache)

    @Throws(IOException::class)
    abstract fun decompress(binaryCache: BinaryCache)

    @Throws(IOException::class)
    abstract fun dataExists(binaryCache: BinaryCache): Boolean

    @Throws(IOException::class)
    abstract fun getSize(binaryCache: BinaryCache): Long

    @Throws(IOException::class)
    abstract fun binaryHash(binaryCache: BinaryCache): Int

    @Throws(IOException::class)
    abstract fun clear(binaryCache: BinaryCache)

    override fun describeContents(): Int {
        return 0
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

        fun canMemoryBeAllocatedInRAM(context: Context, memoryWanted: Long): Boolean {
            val memoryInfo = ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
            val availableMemory = memoryInfo.availMem
            return availableMemory > memoryWanted * 3
        }
    }

}
