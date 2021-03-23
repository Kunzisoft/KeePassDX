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

import android.app.ActivityManager
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Database
import org.apache.commons.io.output.CountingOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher

abstract class BinaryData : Parcelable {

    var isCompressed: Boolean = false
        protected set
    var isProtected: Boolean = false
        protected set
    var isCorrupted: Boolean = false
    private var mLength: Long = 0
    private var mBinaryHash = 0

    // Cipher to encrypt temp file
    @Transient
    protected var cipherEncryption: Cipher = Cipher.getInstance(Database.LoadedKey.BINARY_CIPHER)
    @Transient
    protected var cipherDecryption: Cipher = Cipher.getInstance(Database.LoadedKey.BINARY_CIPHER)

    /**
     * Empty protected binary
     */
    protected constructor()

    protected constructor(compressed: Boolean = false, protected: Boolean = false) {
        this.isCompressed = compressed
        this.isProtected = protected
        this.mLength = 0
        this.mBinaryHash = 0
    }

    protected constructor(parcel: Parcel) {
        isCompressed = parcel.readByte().toInt() != 0
        isProtected = parcel.readByte().toInt() != 0
        isCorrupted = parcel.readByte().toInt() != 0
        mLength = parcel.readLong()
        mBinaryHash = parcel.readInt()
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
    abstract fun delete()

    @Throws(IOException::class)
    abstract fun clear(binaryCache: BinaryCache)

    open fun dataExists(): Boolean {
        return mLength > 0
    }

    fun getSize(): Long {
        return mLength
    }

    fun binaryHash(): Int {
        return mBinaryHash
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (isCompressed) 1 else 0).toByte())
        dest.writeByte((if (isProtected) 1 else 0).toByte())
        dest.writeByte((if (isCorrupted) 1 else 0).toByte())
        dest.writeLong(mLength)
        dest.writeInt(mBinaryHash)
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
        result = 31 * result + mLength.hashCode()
        result = 31 * result + mBinaryHash
        return result
    }

    /**
     * Custom OutputStream to calculate the size and hash of binary file
     */
    protected inner class BinaryCountingOutputStream(out: OutputStream): CountingOutputStream(out) {

        private val mMessageDigest: MessageDigest
        init {
            mLength = 0
            mMessageDigest = MessageDigest.getInstance("MD5")
            mBinaryHash = 0
        }

        override fun beforeWrite(n: Int) {
            super.beforeWrite(n)
            mLength = byteCount
        }

        override fun write(idx: Int) {
            super.write(idx)
            mMessageDigest.update(idx.toByte())
        }

        override fun write(bts: ByteArray) {
            super.write(bts)
            mMessageDigest.update(bts)
        }

        override fun write(bts: ByteArray, st: Int, end: Int) {
            super.write(bts, st, end)
            mMessageDigest.update(bts, st, end)
        }

        override fun close() {
            super.close()
            mLength = byteCount
            val bytes = mMessageDigest.digest()
            mBinaryHash = ByteBuffer.wrap(bytes).int
        }
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
