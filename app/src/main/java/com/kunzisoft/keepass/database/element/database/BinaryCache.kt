package com.kunzisoft.keepass.database.element.database

import com.kunzisoft.keepass.database.element.Database
import java.util.*

class BinaryCache {

    /**
     * Cipher key generated when the database is loaded, and destroyed when the database is closed
     * Can be used to temporarily store database elements
     */
    var loadedCipherKey: Database.LoadedKey? = null

    // Similar to file storage but much faster
    private val byteArrayList = HashMap<Int, ByteArray>()

    fun getByteArray(key: Int?): KeyByteArray {
        if (key == null) {
            val newItem = KeyByteArray(byteArrayList.size, ByteArray(0))
            byteArrayList[newItem.key] = newItem.data
            return newItem
        }
        if (!byteArrayList.containsKey(key)) {
            val newItem = KeyByteArray(key, ByteArray(0))
            byteArrayList[newItem.key] = newItem.data
            return newItem
        }
        return KeyByteArray(key, byteArrayList[key]!!)
    }

    fun setByteArray(key: Int?, data: ByteArray): KeyByteArray {
        return if (key == null) {
            val keyByteArray = KeyByteArray(byteArrayList.size, data)
            byteArrayList[keyByteArray.key] = keyByteArray.data
            keyByteArray
        } else {
            byteArrayList[key] = data
            KeyByteArray(key, data)
        }
    }

    fun removeByteArray(key: Int?) {
        key?.let {
            byteArrayList.remove(it)
        }
    }

    fun clear() {
        byteArrayList.clear()
    }

    data class KeyByteArray(val key: Int, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KeyByteArray) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key
        }
    }
}