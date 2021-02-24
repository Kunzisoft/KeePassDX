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

import java.io.IOException

class BinaryPool {
    private val pool = LinkedHashMap<Int, BinaryAttachment>()

    /**
     * To get a binary by the pool key (ref attribute in entry)
     */
    operator fun get(key: Int): BinaryAttachment? {
        return pool[key]
    }

    /**
     * To linked a binary with a pool key, if the pool key doesn't exists, create an unused one
     */
    fun put(key: Int?, value: BinaryAttachment) {
        if (key == null)
            put(value)
        else
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

    /**
     * Remove a binary from the pool, the file is not deleted
     */
    @Throws(IOException::class)
    fun remove(binaryAttachment: BinaryAttachment) {
        findKey(binaryAttachment)?.let {
            pool.remove(it)
        }
        // Don't clear attachment here because a file can be used in many BinaryAttachment
    }

    /**
     * Utility method to find an unused key in the pool
     */
    private fun findUnusedKey(): Int {
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
     * Utility method to order binaries and solve index problem in database v4
     */
    private fun orderedBinaries(): List<KeyBinary> {
        val keyBinaryList = ArrayList<KeyBinary>()
        for ((key, binary) in pool) {
            // Don't deduplicate
            val existentBinary = keyBinaryList.find { it.binary.md5() == binary.md5() }
            if (existentBinary == null) {
                keyBinaryList.add(KeyBinary(binary, key))
            } else {
                existentBinary.addKey(key)
            }
        }
        return keyBinaryList
    }

    /**
     * To register a binary with a ref corresponding to an ordered index
     */
    fun getBinaryIndexFromKey(key: Int): Int? {
        val index = orderedBinaries().indexOfFirst { it.keys.contains(key) }
        return if (index < 0)
            null
        else
            index
    }

    /**
     * Different from doForEach, provide an ordered index to each binary
     */
    fun doForEachOrderedBinary(action: (index: Int, binary: BinaryAttachment) -> Unit) {
        orderedBinaries().forEachIndexed { index, keyBinary ->
            action.invoke(index, keyBinary.binary)
        }
    }

    /**
     * To do an action on each binary in the pool
     */
    fun doForEachBinary(action: (binary: BinaryAttachment) -> Unit) {
        pool.values.forEach { action.invoke(it) }
    }

    @Throws(IOException::class)
    fun clear() {
        doForEachBinary {
            it.clear()
        }
        pool.clear()
    }

    override fun toString(): String {
        val stringBuffer = StringBuffer()
        for ((key, value) in pool) {
            if (stringBuffer.isNotEmpty())
                stringBuffer.append(", {$key:$value}")
            else
                stringBuffer.append("{$key:$value}")
        }
        return stringBuffer.toString()
    }

    /**
     * Utility class to order binaries
     */
    private class KeyBinary(val binary: BinaryAttachment, key: Int) {
        val keys = HashSet<Int>()
        init {
            addKey(key)
        }

        fun addKey(key: Int) {
            keys.add(key)
        }
    }
}
