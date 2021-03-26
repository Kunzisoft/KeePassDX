package com.kunzisoft.keepass.database.element.binary

import java.io.Serializable
import java.security.Key
import java.security.SecureRandom
import javax.crypto.KeyGenerator

class LoadedKey(val key: Key, val iv: ByteArray): Serializable {
    companion object {
        const val BINARY_CIPHER = "Blowfish/CBC/PKCS5Padding"

        fun generateNewCipherKey(): LoadedKey {
            val iv = ByteArray(8)
            SecureRandom().nextBytes(iv)
            return LoadedKey(KeyGenerator.getInstance("Blowfish").generateKey(), iv)
        }
    }
}