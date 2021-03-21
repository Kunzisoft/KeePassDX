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

import com.kunzisoft.encrypt.finalkey.AndroidAESKeyTransformer
import com.kunzisoft.encrypt.finalkey.NativeAESKeyTransformer
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESTest {

    private val mRand = Random()

    @Test
    fun testAES() {
        // Test both an old and an even number to test my flip variable
        testAESFinalKey(5)
        testAESFinalKey(6)
    }

    private fun testAESFinalKey(rounds: Long) {
        val seed = ByteArray(32)
        val key = ByteArray(32)
        val nativeKey: ByteArray?
        val androidKey: ByteArray?

        mRand.nextBytes(seed)
        mRand.nextBytes(key)

        val androidAESKey = AndroidAESKeyTransformer()
        androidKey = androidAESKey.transformMasterKey(seed, key, rounds)

        val nativeAESKey = NativeAESKeyTransformer()
        nativeKey = nativeAESKey.transformMasterKey(seed, key, rounds)

        assertArrayEquals("Does not match", androidKey, nativeKey)
    }

    @Test
    fun testEncrypt() {
        // Test above below and at the blocksize
        testFinal(15)
        testFinal(16)
        testFinal(17)

        // Test random larger sizes
        val size = mRand.nextInt(494) + 18
        testFinal(size)
    }

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
