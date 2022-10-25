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
package com.kunzisoft.keepass.database.element.binary

import android.util.Log
import java.io.IOException
import kotlin.math.abs

abstract class BinaryPool<T> {

    protected val pool = LinkedHashMap<T, BinaryData>()

    // To build unique file id
    private var creationId: Long = System.currentTimeMillis()
    private var poolId: Int = abs(javaClass.simpleName.hashCode())
    private var binaryFileIncrement = 0L

    /**
     * To get a binary by the pool key (ref attribute in entry)
     */
    operator fun get(key: T): BinaryData? {
        return pool[key]
    }

    /**
     * Create and return a new binary file not yet linked to a binary
     */
    fun put(key: T? = null,
            builder: (uniqueBinaryId: String) -> BinaryData): KeyBinary<T> {
        binaryFileIncrement++
        val newBinaryFile: BinaryData = builder("$poolId$creationId$binaryFileIncrement")
        val newKey = put(key, newBinaryFile)
        return KeyBinary(newBinaryFile, newKey)
    }

    /**
     * To linked a binary with a pool key, if the pool key doesn't exists, create an unused one
     */
    fun put(key: T?, value: BinaryData): T {
        if (key == null)
            return put(value)
        else
            pool[key] = value
        return key
    }

    /**
     * To put a [binaryData] in the pool,
     * if already exists, replace the current one,
     * else add it with a new key
     */
    fun put(binaryData: BinaryData): T {
        var key: T? = findKey(binaryData)
        if (key == null) {
            key = findUnusedKey()
        }
        pool[key!!] = binaryData
        return key
    }

    /**
     * Remove a binary from the pool with its [key], the file is not deleted
     */
    @Throws(IOException::class)
    fun remove(key: T) {
        pool.remove(key)
        // Don't clear attachment here because a file can be used in many BinaryAttachment
    }

    /**
     * Remove a binary from the pool, the file is not deleted
     */
    @Throws(IOException::class)
    fun remove(binaryData: BinaryData) {
        findKey(binaryData)?.let {
            pool.remove(it)
        }
        // Don't clear attachment here because a file can be used in many BinaryAttachment
    }

    /**
     * Utility method to find an unused key in the pool
     */
    abstract fun findUnusedKey(): T

    /**
     * Return key of [binaryDataToRetrieve] or null if not found
     */
    private fun findKey(binaryDataToRetrieve: BinaryData): T? {
        val contains = pool.containsValue(binaryDataToRetrieve)
        return if (!contains)
            null
        else {
            for ((key, binary) in pool) {
                if (binary == binaryDataToRetrieve) {
                    return key
                }
            }
            return null
        }
    }

    fun isBinaryDuplicate(binaryData: BinaryData?): Boolean {
        try {
            binaryData?.let {
                if (it.getSize() > 0) {
                    val searchBinaryMD5 = it.binaryHash()
                    var i = 0
                    for ((_, binary) in pool) {
                        if (binary.binaryHash() == searchBinaryMD5) {
                            i++
                            if (i > 1)
                                return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to check binary duplication", e)
        }
        return false
    }

    /**
     * To do an action on each binary in the pool (order is not important)
     */
    private fun doForEachBinary(action: (key: T, binary: BinaryData) -> Unit,
                                condition: (key: T, binary: BinaryData) -> Boolean) {
        for ((key, value) in pool) {
            if (condition.invoke(key, value)) {
                action.invoke(key, value)
            }
        }
    }

    fun doForEachBinary(action: (key: T, binary: BinaryData) -> Unit) {
        doForEachBinary(action) { _, _ -> true }
    }

    /**
     * Utility method to order binaries and solve index problem in database v4
     */
    protected fun orderedBinariesWithoutDuplication(condition: ((binary: BinaryData) -> Boolean) = { true })
    : List<KeyBinary<T>> {
        val keyBinaryList = ArrayList<KeyBinary<T>>()
        for ((key, binary) in pool) {
            // Don't deduplicate
            val existentBinary =
            try {
                if (binary.getSize() > 0) {
                    keyBinaryList.find {
                        val hash0 = it.binary.binaryHash()
                        val hash1 = binary.binaryHash()
                        hash0 != 0 && hash1 != 0 && hash0 == hash1
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to check binary hash", e)
                null
            }
            if (existentBinary == null) {
                val newKeyBinary = KeyBinary(binary, key)
                if (condition.invoke(newKeyBinary.binary)) {
                    keyBinaryList.add(newKeyBinary)
                }
            } else {
                if (condition.invoke(existentBinary.binary)) {
                    existentBinary.addKey(key)
                }
            }
        }
        return keyBinaryList
    }

    /**
     * Different from doForEach, provide an ordered index to each binary
     */
    private fun doForEachBinaryWithoutDuplication(action: (keyBinary: KeyBinary<T>) -> Unit,
                                                  conditionToAdd: (binary: BinaryData) -> Boolean) {
        orderedBinariesWithoutDuplication(conditionToAdd).forEach { keyBinary ->
            action.invoke(keyBinary)
        }
    }

    fun doForEachBinaryWithoutDuplication(action: (keyBinary: KeyBinary<T>) -> Unit) {
        doForEachBinaryWithoutDuplication(action, { true })
    }

    /**
     * Different from doForEach, provide an ordered index to each binary
     */
    private fun doForEachOrderedBinaryWithoutDuplication(action: (index: Int, binary: BinaryData) -> Unit,
                                                         conditionToAdd: (binary: BinaryData) -> Boolean) {
        orderedBinariesWithoutDuplication(conditionToAdd).forEachIndexed { index, keyBinary ->
            action.invoke(index, keyBinary.binary)
        }
    }

    fun doForEachOrderedBinaryWithoutDuplication(action: (index: Int, binary: BinaryData) -> Unit) {
        doForEachOrderedBinaryWithoutDuplication(action, { true })
    }

    fun isEmpty(): Boolean {
        return pool.isEmpty()
    }

    @Throws(IOException::class)
    fun clear() {
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
    class KeyBinary<T>(val binary: BinaryData, key: T) {
        val keys = HashSet<T>()
        init {
            addKey(key)
        }

        fun addKey(key: T) {
            keys.add(key)
        }
    }

    companion object {
        private val TAG = BinaryPool::class.java.name
    }
}
