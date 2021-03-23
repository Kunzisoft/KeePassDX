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
import com.kunzisoft.keepass.database.element.Database
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

abstract class BinaryData {

    var isCompressed: Boolean = false
        protected set
    var isProtected: Boolean = false
        protected set
    var isCorrupted: Boolean = false

    /**
     * Empty protected binary
     */
    protected constructor()

    protected constructor(compressed: Boolean = false, protected: Boolean = false) {
        this.isCompressed = compressed
        this.isProtected = protected
    }

    @Throws(IOException::class)
    abstract fun getInputDataStream(cipherKey: Database.LoadedKey): InputStream

    @Throws(IOException::class)
    abstract fun getOutputDataStream(cipherKey: Database.LoadedKey): OutputStream

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
    abstract fun compress(cipherKey: Database.LoadedKey)

    @Throws(IOException::class)
    abstract fun decompress(cipherKey: Database.LoadedKey)

    @Throws(IOException::class)
    abstract fun clear()

    abstract fun dataExists(): Boolean

    abstract fun getSize(): Long

    abstract fun binaryHash(): Int

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
