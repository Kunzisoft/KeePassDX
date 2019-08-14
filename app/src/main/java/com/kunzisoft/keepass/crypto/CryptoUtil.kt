/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto

import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.stream.NullOutputStream

import java.io.IOException
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

import javax.crypto.Mac
import kotlin.math.min

object CryptoUtil {

    fun resizeKey(inBytes: ByteArray, inOffset: Int, cbIn: Int, cbOut: Int): ByteArray {
        if (cbOut == 0) return ByteArray(0)

        val hash: ByteArray = if (cbOut <= 32) {
            hashSha256(inBytes, inOffset, cbIn)
        } else {
            hashSha512(inBytes, inOffset, cbIn)
        }

        if (cbOut == hash.size) {
            return hash
        }

        val ret = ByteArray(cbOut)
        if (cbOut < hash.size) {
            System.arraycopy(hash, 0, ret, 0, cbOut)
        } else {
            var pos = 0
            var r: Long = 0
            while (pos < cbOut) {
                val hmac: Mac
                try {
                    hmac = Mac.getInstance("HmacSHA256")
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                }

                val pbR = LEDataOutputStream.writeLongBuf(r)
                val part = hmac.doFinal(pbR)

                val copy = min(cbOut - pos, part.size)
                System.arraycopy(part, 0, ret, pos, copy)
                pos += copy
                r++

                Arrays.fill(part, 0.toByte())
            }
        }

        Arrays.fill(hash, 0.toByte())
        return ret
    }

    @JvmOverloads
    fun hashSha256(data: ByteArray, offset: Int = 0, count: Int = data.size): ByteArray {
        return hashGen("SHA-256", data, offset, count)
    }

    @JvmOverloads
    fun hashSha512(data: ByteArray, offset: Int = 0, count: Int = data.size): ByteArray {
        return hashGen("SHA-512", data, offset, count)
    }

    private fun hashGen(transform: String, data: ByteArray, offset: Int, count: Int): ByteArray {
        val hash: MessageDigest
        try {
            hash = MessageDigest.getInstance(transform)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

        val nos = NullOutputStream()
        val dos = DigestOutputStream(nos, hash)

        try {
            dos.write(data, offset, count)
            dos.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return hash.digest()
    }
}
