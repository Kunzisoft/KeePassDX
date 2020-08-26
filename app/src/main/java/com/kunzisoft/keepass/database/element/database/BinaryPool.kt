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

import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import java.io.IOException

class BinaryPool {
    private val pool = LinkedHashMap<Int, BinaryAttachment>()

    operator fun get(key: Int): BinaryAttachment? {
        return pool[key]
    }

    fun put(key: Int, value: BinaryAttachment) {
        pool[key] = value
    }

    /**
     * To put a [binaryAttachment] in the pool,
     * if already exists, replace the current one,
     * else add it with a new key
     */
    fun put(binaryAttachment: BinaryAttachment): Int {
        var key = findKey(binaryAttachment)
        if (key == null) {
            key = findUnusedKey()
        }
        pool[key] = binaryAttachment
        return key
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
     * Return key of [binaryAttachmentToRetrieve] or null if not found
     */
    private fun findKey(binaryAttachmentToRetrieve: BinaryAttachment): Int? {
        val contains = pool.containsValue(binaryAttachmentToRetrieve)
        return if (!contains)
            null
        else {
            for ((key, binary) in pool) {
                if (binary == binaryAttachmentToRetrieve) {
                    return key
                }
            }
            return null
        }
    }

    /**
     * Warning 2 keys can point the same binary
     */
    fun doForEach(action: (key: Int, binary: BinaryAttachment) -> Unit) {
        for ((key, binary) in pool) {
            action.invoke(key, binary)
        }
    }

    fun doForEachBinary(action: (binary: BinaryAttachment) -> Unit) {
        pool.values.toSet().forEach { action.invoke(it) }
    }

    @Throws(IOException::class)
    fun clear() {
        doForEachBinary {
            it.clear()
        }
        pool.clear()
    }
}
