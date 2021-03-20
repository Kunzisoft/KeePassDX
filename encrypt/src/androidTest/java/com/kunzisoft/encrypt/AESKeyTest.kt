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
package com.kunzisoft.encrypt

import org.junit.Assert.assertArrayEquals

import java.io.IOException
import java.util.Random

import junit.framework.TestCase

import com.kunzisoft.encrypt.finalkey.AndroidAESKeyTransformer
import com.kunzisoft.encrypt.finalkey.NativeAESKeyTransformer

class AESKeyTest : TestCase() {
    private lateinit var mRand: Random

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        mRand = Random()
    }

    @Throws(IOException::class)
    fun testAES() {
        // Test both an old and an even number to test my flip variable
        testAESFinalKey(5)
        testAESFinalKey(6)
    }

    @Throws(IOException::class)
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
}
