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
package com.kunzisoft.keepass.database.crypto

import java.io.IOException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacBlock {

    fun getHmacSha256(blockKey: ByteArray): Mac {
        val hmac: Mac
        try {
            hmac = Mac.getInstance("HmacSHA256")
            val signingKey = SecretKeySpec(blockKey, "HmacSHA256")
            hmac.init(signingKey)
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("No HmacAlogirthm")
        } catch (e: InvalidKeyException) {
            throw IOException("Invalid Hmac Key")
        }
        return hmac
    }

    fun getHmacKey64(key: ByteArray, blockIndex: ByteArray): ByteArray {
        val hash: MessageDigest
        try {
            hash = MessageDigest.getInstance("SHA-512")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        hash.update(blockIndex)
        hash.update(key)
        return hash.digest()
    }
}
