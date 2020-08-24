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

    fun add(fileBinary: BinaryAttachment) {
        if (findKey(fileBinary) == null) {
            pool.put(findUnusedKey(), fileBinary)
        }
    }

    fun remove(fileBinary: BinaryAttachment) {
        findKey(fileBinary)?.let {
            pool.remove(it)
        }
    }

    fun findUnusedKey(): Int {
        var unusedKey = pool.size()
        while (pool[unusedKey] != null)
            unusedKey++
        return unusedKey
    }

    /**
     * Return position of [binaryAttachmentToRetrieve] or null if not found
     */
    fun findKey(binaryAttachmentToRetrieve: BinaryAttachment): Int? {
        for (i in 0 until pool.size()) {
            if (pool.get(pool.keyAt(i)) == binaryAttachmentToRetrieve) return i
        }
        return null
    }

    fun doForEachBinary(action: (key: Int, binary: BinaryAttachment) -> Unit) {
        for (i in 0 until pool.size()) {
            action.invoke(i, pool.get(pool.keyAt(i)))
        }
    }

    @Throws(IOException::class)
    fun clear() {
        doForEachBinary { _, binary ->
            binary.clear()
        }
        pool.clear()
    }
}
