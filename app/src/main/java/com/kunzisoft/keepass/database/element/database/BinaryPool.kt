/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import android.util.SparseArray
import androidx.core.util.forEach
import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import java.io.IOException

class BinaryPool {
    private val pool = SparseArray<BinaryAttachment>()

    operator fun get(key: Int): BinaryAttachment? {
        return pool[key]
    }

    fun put(key: Int, value: BinaryAttachment) {
        pool.put(key, value)
    }

    fun add(binaryAttachment: BinaryAttachment) {
        if (findKey(binaryAttachment) == null) {
            pool.put(findUnusedKey(), binaryAttachment)
        }
    }

    @Throws(IOException::class)
    fun remove(binaryAttachment: BinaryAttachment) {
        findKey(binaryAttachment)?.let {
            pool.remove(it)
        }
        binaryAttachment.clear()
    }

    fun findUnusedKey(): Int {
        var unusedKey = 0
        while (pool[unusedKey] != null)
            unusedKey++
        return unusedKey
    }

    /**
     * Return position of [binaryAttachmentToRetrieve] or null if not found
     */
    fun findKey(binaryAttachmentToRetrieve: BinaryAttachment): Int? {
        val index = pool.indexOfValue(binaryAttachmentToRetrieve)
        return if (index < 0)
            null
        else
            pool.keyAt(index)
    }

    fun doForEachBinary(action: (key: Int, binary: BinaryAttachment) -> Unit) {
        pool.forEach { key, binaryAttachment ->
            action.invoke(key, binaryAttachment)
        }
    }

    @Throws(IOException::class)
    fun clear() {
        pool.forEach { _, binary ->
            binary.clear()
        }
        pool.clear()
    }
}
