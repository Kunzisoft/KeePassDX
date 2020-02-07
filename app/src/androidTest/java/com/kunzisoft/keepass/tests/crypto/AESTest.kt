/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.tests.crypto

import com.kunzisoft.keepass.crypto.CipherFactory

import junit.framework.TestCase

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Random

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import org.junit.Assert.assertArrayEquals

class AESTest : TestCase() {

    private val mRand = Random()

    @Throws(InvalidKeyException::class, NoSuchAlgorithmException::class, NoSuchPaddingException::class, IllegalBlockSizeException::class, BadPaddingException::class, InvalidAlgorithmParameterException::class)
    fun testEncrypt() {
        // Test above below and at the blocksize
        testFinal(15)
        testFinal(16)
        testFinal(17)

        // Test random larger sizes
        val size = mRand.nextInt(494) + 18
        testFinal(size)
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, IllegalBlockSizeException::class, BadPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    private fun testFinal(dataSize: Int) {

        // Generate some input
        val input = ByteArray(dataSize)
        mRand.nextBytes(input)

        // Generate key
        val keyArray = ByteArray(32)
        mRand.nextBytes(keyArray)
        val key = SecretKeySpec(keyArray, "AES")

        // Generate IV
        val ivArray = ByteArray(16)
        mRand.nextBytes(ivArray)
        val iv = IvParameterSpec(ivArray)

        val android = CipherFactory.getInstance("AES/CBC/PKCS5Padding", true)
        android.init(Cipher.ENCRYPT_MODE, key, iv)
        val outAndroid = android.doFinal(input, 0, dataSize)

        val nat = CipherFactory.getInstance("AES/CBC/PKCS5Padding")
        nat.init(Cipher.ENCRYPT_MODE, key, iv)
        val outNative = nat.doFinal(input, 0, dataSize)

        assertArrayEquals("Arrays differ on size: $dataSize", outAndroid, outNative)
    }


}
