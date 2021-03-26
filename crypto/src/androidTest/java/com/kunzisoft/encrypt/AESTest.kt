/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePassDX. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.encrypt

import com.kunzisoft.encrypt.aes.AESTransformer
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

class AESTest {

    private val mRand = Random()

    @Test
    fun testAESByteArray() {
        // Generate random input
        val input = ByteArray(mRand.nextInt(494) + 18)
        mRand.nextBytes(input)
        // Generate key
        val keyArray = ByteArray(32)
        mRand.nextBytes(keyArray)
        // Generate IV
        val ivArray = ByteArray(16)
        mRand.nextBytes(ivArray)

        val androidEncrypt = CipherFactory.getAES(Cipher.ENCRYPT_MODE, keyArray, ivArray).doFinal(input)
        val nativeEncrypt = CipherFactory.getAES(Cipher.ENCRYPT_MODE, keyArray, ivArray, true).doFinal(input)

        assertArrayEquals("Check AES encryption", androidEncrypt, nativeEncrypt)

        val androidDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray).doFinal(androidEncrypt)
        val nativeDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray, true).doFinal(nativeEncrypt)

        assertArrayEquals("Check AES encryption/decryption", androidDecrypt, nativeDecrypt)

        val androidMixDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray).doFinal(nativeEncrypt)
        val nativeMixDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray, true).doFinal(androidEncrypt)

        assertArrayEquals("Check AES mix encryption/decryption", androidMixDecrypt, nativeMixDecrypt)
    }

    @Test
    fun testAESStream() {
        // Generate random input
        val input = ByteArray(mRand.nextInt(494) + 18)
        mRand.nextBytes(input)
        // Generate key
        val keyArray = ByteArray(32)
        mRand.nextBytes(keyArray)
        // Generate IV
        val ivArray = ByteArray(16)
        mRand.nextBytes(ivArray)

        val androidEncrypt = CipherFactory.getAES(Cipher.ENCRYPT_MODE, keyArray, ivArray)
        val androidDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray)
        val androidOutputStream = ByteArrayOutputStream()
        CipherInputStream(ByteArrayInputStream(input), androidEncrypt).use { cipherInputStream ->
            CipherOutputStream(androidOutputStream, androidDecrypt).use { outputStream ->
                outputStream.write(cipherInputStream.readBytes())
            }
        }
        val androidOut = androidOutputStream.toByteArray()

        val nativeEncrypt = CipherFactory.getAES(Cipher.ENCRYPT_MODE, keyArray, ivArray)
        val nativeDecrypt = CipherFactory.getAES(Cipher.DECRYPT_MODE, keyArray, ivArray)
        val nativeOutputStream = ByteArrayOutputStream()
        CipherInputStream(ByteArrayInputStream(input), nativeEncrypt).use { cipherInputStream ->
            CipherOutputStream(nativeOutputStream, nativeDecrypt).use { outputStream ->
                outputStream.write(cipherInputStream.readBytes())
            }
        }
        val nativeOut = nativeOutputStream.toByteArray()

        assertArrayEquals("Check AES encryption/decryption", androidOut, nativeOut)
    }

    @Test
    fun testAESKDF() {
        val seed = ByteArray(32)
        mRand.nextBytes(seed)
        val key = ByteArray(32)
        mRand.nextBytes(key)
        val rounds = 60000L

        val androidKey = AESTransformer.transformKeyInJVM(seed, key, rounds)
        val nativeKey = AESTransformer.transformKey(seed, key, rounds)

        assertArrayEquals("Does not match", androidKey, nativeKey)
    }
}
