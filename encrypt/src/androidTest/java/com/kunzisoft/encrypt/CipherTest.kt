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

import com.kunzisoft.encrypt.engine.AesEngine
import com.kunzisoft.encrypt.stream.BetterCipherInputStream
import com.kunzisoft.encrypt.stream.LittleEndianDataInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

class CipherTest {
    private val rand = Random()

    @Test
    fun testCipherFactory() {
        val key = ByteArray(32)
        val iv = ByteArray(16)

        val plaintext = ByteArray(1024)

        rand.nextBytes(key)
        rand.nextBytes(iv)
        rand.nextBytes(plaintext)

        val aes = CipherFactory.getInstance(AesEngine.CIPHER_UUID)
        val encrypt = aes.getCipher(Cipher.ENCRYPT_MODE, key, iv)
        val decrypt = aes.getCipher(Cipher.DECRYPT_MODE, key, iv)

        val secrettext = encrypt.doFinal(plaintext)
        val decrypttext = decrypt.doFinal(secrettext)

        assertArrayEquals("Encryption and decryption failed", plaintext, decrypttext)
    }

    @Test
    fun testCipherStreams() {
        val MESSAGE_LENGTH = 1024

        val key = ByteArray(32)
        val iv = ByteArray(16)

        val plaintext = ByteArray(MESSAGE_LENGTH)

        rand.nextBytes(key)
        rand.nextBytes(iv)
        rand.nextBytes(plaintext)

        val aes = CipherFactory.getInstance(AesEngine.CIPHER_UUID)
        val encrypt = aes.getCipher(Cipher.ENCRYPT_MODE, key, iv)
        val decrypt = aes.getCipher(Cipher.DECRYPT_MODE, key, iv)

        val bos = ByteArrayOutputStream()
        val cos = CipherOutputStream(bos, encrypt)
        cos.write(plaintext)
        cos.close()

        val secrettext = bos.toByteArray()

        val bis = ByteArrayInputStream(secrettext)
        val cis = BetterCipherInputStream(bis, decrypt)
        val lis = LittleEndianDataInputStream(cis)

        val decrypttext = lis.readBytes(MESSAGE_LENGTH)

        assertArrayEquals("Encryption and decryption failed", plaintext, decrypttext)
    }
}
