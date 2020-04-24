/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.crypto.finalkey

import java.io.IOException
import java.lang.Exception
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.ShortBufferException
import javax.crypto.spec.SecretKeySpec

class AndroidAESKeyTransformer : KeyTransformer() {
    @Throws(IOException::class)
    override fun transformMasterKey(seed: ByteArray?, key: ByteArray?, rounds: Long?): ByteArray? {
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
        val messageDigest: MessageDigest = try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here: " + e.message)
        }
        messageDigest.update(newKey)
        return messageDigest.digest()
    }
}