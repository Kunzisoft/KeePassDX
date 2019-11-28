/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 * KeePass DX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePass DX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePass DX. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.tests.crypto

import org.junit.Assert.assertArrayEquals

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Random

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

import junit.framework.TestCase

import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.engine.AesEngine
import com.kunzisoft.keepass.stream.BetterCipherInputStream
import com.kunzisoft.keepass.stream.LittleEndianDataInputStream

class CipherTest : TestCase() {
    private val rand = Random()

    @Throws(InvalidKeyException::class, NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidAlgorithmParameterException::class, IllegalBlockSizeException::class, BadPaddingException::class)
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

    @Throws(InvalidKeyException::class, NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidAlgorithmParameterException::class, IllegalBlockSizeException::class, BadPaddingException::class, IOException::class)
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
