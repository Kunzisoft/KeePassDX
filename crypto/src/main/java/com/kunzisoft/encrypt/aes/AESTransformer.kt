/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.encrypt.aes

import android.annotation.SuppressLint
import android.util.Log
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.encrypt.NativeLib
import java.io.IOException
import java.security.InvalidKeyException
import javax.crypto.Cipher
import javax.crypto.ShortBufferException
import javax.crypto.spec.SecretKeySpec

object AESTransformer {

    fun transformKey(seed: ByteArray?, key: ByteArray?, rounds: Long?): ByteArray? {
        // Prefer the native final key implementation
        return try {
            NativeLib.init()
            NativeAESKeyTransformer.nTransformKey(seed, key, rounds!!)
        } catch (exception: Exception) {
            Log.e(AESTransformer::class.java.simpleName, "Unable to perform native AES key transformation", exception)
            // Fall back on the android crypto implementation
            transformKeyInJVM(seed, key, rounds)
        }
    }

    @SuppressLint("GetInstance")
    @Throws(IOException::class)
    fun transformKeyInJVM(seed: ByteArray?, key: ByteArray?, rounds: Long?): ByteArray {
        val cipher: Cipher = try {
            Cipher.getInstance("AES/ECB/NoPadding")
        } catch (e: Exception) {
            throw IOException("Unable to get the cipher", e)
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(seed, "AES"))
        } catch (e: InvalidKeyException) {
            throw IOException("Unable to init the cipher", e)
        }
        if (key == null) {
            throw IOException("Invalid key")
        }
        if (rounds == null) {
            throw IOException("Invalid rounds")
        }

        // Encrypt key rounds times
        val keyLength = key.size
        val newKey = ByteArray(keyLength)
        System.arraycopy(key, 0, newKey, 0, keyLength)
        val destKey = ByteArray(keyLength)
        for (i in 0 until rounds) {
            try {
                cipher.update(newKey, 0, newKey.size, destKey, 0)
                System.arraycopy(destKey, 0, newKey, 0, newKey.size)
            } catch (e: ShortBufferException) {
                throw IOException("Short buffer", e)
            }
        }

        // Hash the key
        return HashManager.hashSha256(newKey)
    }
}