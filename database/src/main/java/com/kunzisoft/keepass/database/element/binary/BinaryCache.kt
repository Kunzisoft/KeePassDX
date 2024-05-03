package com.kunzisoft.keepass.database.element.binary

import java.io.File
import java.util.*

class BinaryCache {

    /**
     * Cipher key generated when the database is loaded, and destroyed when the database is closed
     * Can be used to temporarily store database elements
     */
    var loadedCipherKey: LoadedKey = LoadedKey.generateNewCipherKey()

    var cacheDirectory: File? = null

    private val voidBinary = KeyByteArray(UNKNOWN, ByteArray(0))

    fun getBinaryData(binaryId: String,
                      smallSize: Boolean = false,
                      compression: Boolean = false,
                      protection: Boolean = false): BinaryData {
        val cacheDir = cacheDirectory
        return if (smallSize || cacheDir == null) {
            BinaryByte(binaryId, compression, protection)
        } else {
            val fileInCache = File(cacheDir, binaryId)
            BinaryFile(fileInCache, compression, protection)
        }
    }

    // Similar to file storage but much faster TODO SparseArray
    private val byteArrayList = HashMap<String, ByteArray>()

    fun getByteArray(key: String): KeyByteArray {
        if (key == UNKNOWN) {
            return voidBinary
        }
        if (!byteArrayList.containsKey(key)) {
            val newItem = KeyByteArray(key, ByteArray(0))
            byteArrayList[newItem.key] = newItem.data
            return newItem
        }
        return KeyByteArray(key, byteArrayList[key]!!)
    }

    fun setByteArray(key: String, data: ByteArray): KeyByteArray {
        if (key == UNKNOWN) {
            return voidBinary
        }
        byteArrayList[key] = data
        return KeyByteArray(key, data)
    }

    fun removeByteArray(key: String?) {
        key?.let {
            byteArrayList.remove(it)
        }
    }

    fun clear() {
        byteArrayList.clear()
    }

    companion object {
        const val UNKNOWN = "UNKNOWN"
    }

    data class KeyByteArray(val key: String, val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KeyByteArray) return false

            if (key != other.key) return false

            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }
}