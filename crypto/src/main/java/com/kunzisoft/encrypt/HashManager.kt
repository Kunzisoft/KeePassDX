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
package com.kunzisoft.encrypt

import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.IOException
import java.security.InvalidKeyException
import java.security.Key
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Manager for hash and HMAC operations.
 */
object HashManager {

    /**
     * Generate random bytes using SecureRandom.
     *
     * @param size Number of random bytes to generate
     * @return Generated random bytes
     */
    fun generateRandom(size: Int): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * Get an initialized MessageDigest for SHA-256.
     *
     * @return MessageDigest for SHA-256
     * @throws IOException if algorithm is not found
     */
    fun getSha256(): MessageDigest {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.", e)
        }
        return messageDigest
    }

    /**
     * Calculate a SHA-256 hash of the provided data.
     *
     * @param data Variable number of ByteArrays to hash
     * @return SHA-256 hash result
     */
    fun sha256(vararg data: ByteArray?): ByteArray {
        val hash: MessageDigest = getSha256()
        for (byteArray in data) {
            if (byteArray != null)
                hash.update(byteArray)
        }
        return hash.digest()
    }



    /**
     * Get an initialized HMAC-SHA256 Mac instance.
     *
     * @param key Secret key for HMAC
     * @return Initialized Mac instance
     */
    fun getHmacSha256(key: ByteArray): Mac {
        return getHmacSha256(SecretKeySpec(key, "HmacSHA256"))
    }

    /**
     * Get an initialized HMAC-SHA256 Mac instance from a Key.
     *
     * @param key Secret key for HMAC
     * @return Initialized Mac instance
     * @throws IOException if algorithm is not found or key is invalid
     */
    fun getHmacSha256(key: Key): Mac {
        val hmac: Mac = try {
            Mac.getInstance("HmacSHA256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("HmacSHA256 not implemented here.", e)
        }
        try {
            hmac.init(key)
        } catch (e: InvalidKeyException) {
            throw IOException("Invalid Hmac Key", e)
        }
        return hmac
    }

    /**
     * Calculate HMAC-SHA256 of the provided data.
     *
     * @param key Secret key for HMAC
     * @param data Data to authenticate
     * @return HMAC-SHA256 result
     */
    fun hmacSha256(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        return getHmacSha256(key).doFinal(data)
    }

    /**
     * Get an initialized MessageDigest for SHA-512.
     *
     * @return MessageDigest for SHA-512
     * @throws IOException if algorithm is not found
     */
    fun getSha512(): MessageDigest {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-512")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-512 not implemented here.", e)
        }
        return messageDigest
    }

    /**
     * Calculate a SHA-512 hash of the provided data.
     * Also used for deriving KDBX HMAC block keys.
     *
     * @param data Variable number of ByteArrays to hash
     * @return SHA-512 hash result
     */
    fun sha512(vararg data: ByteArray?): ByteArray {
        val hash: MessageDigest = getSha512()
        for (byteArray in data) {
            if (byteArray != null)
                hash.update(byteArray)
        }
        return hash.digest()
    }

    /**
     * Get a Salsa20 stream cipher instance.
     *
     * @param key Master key to derive the cipher key
     * @return Initialized Salsa20 StreamCipher
     */
    fun getSalsa20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val key32 = sha256(key)

        val keyParam = KeyParameter(key32)

        val salsaIV = byteArrayOf(
            0xE8.toByte(),
            0x30,
            0x09,
            0x4B,
            0x97.toByte(),
            0x20,
            0x5D,
            0x2A
        )
        val ivParam = ParametersWithIV(keyParam, salsaIV)

        val cipher = Salsa20Engine()
        cipher.init(true, ivParam)

        return StreamCipher(cipher)
    }

    /**
     * Get a ChaCha20 stream cipher instance.
     *
     * @param key Master key to derive the cipher key and IV
     * @return Initialized ChaCha20 StreamCipher
     */
    fun getChaCha20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val hash = sha512(key)
        val key32 = ByteArray(32)
        val iv = ByteArray(12)

        System.arraycopy(hash, 0, key32, 0, 32)
        System.arraycopy(hash, 32, iv, 0, 12)

        val keyParam = KeyParameter(key32)
        val ivParam = ParametersWithIV(keyParam, iv)

        val cipher = ChaCha7539Engine()
        cipher.init(true, ivParam)

        return StreamCipher(cipher)
    }
}
