/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.crypto

import org.bouncycastle.crypto.StreamCipher
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV

object StreamCipherFactory {

    private val SALSA_IV = byteArrayOf(0xE8.toByte(), 0x30, 0x09, 0x4B, 0x97.toByte(), 0x20, 0x5D, 0x2A)

    fun getInstance(alg: CrsAlgorithm?, key: ByteArray): StreamCipher? {
        return when {
            alg === CrsAlgorithm.Salsa20 -> getSalsa20(key)
            alg === CrsAlgorithm.ChaCha20 -> getChaCha20(key)
            else -> null
        }
    }

    private fun getSalsa20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val key32 = CryptoUtil.hashSha256(key)

        val keyParam = KeyParameter(key32)
        val ivParam = ParametersWithIV(keyParam, SALSA_IV)

        val cipher = Salsa20Engine()
        cipher.init(true, ivParam)

        return cipher
    }

    private fun getChaCha20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val hash = CryptoUtil.hashSha512(key)
        val key32 = ByteArray(32)
        val iv = ByteArray(12)

        System.arraycopy(hash, 0, key32, 0, 32)
        System.arraycopy(hash, 32, iv, 0, 12)

        val keyParam = KeyParameter(key32)
        val ivParam = ParametersWithIV(keyParam, iv)

        val cipher = ChaCha7539Engine()
        cipher.init(true, ivParam)

        return cipher
    }
}
