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
package com.kunzisoft.keepass.database.crypto.kdf

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.encrypt.aes.AESTransformer
import com.kunzisoft.keepass.utils.UnsignedLong
import com.kunzisoft.keepass.utils.bytes16ToUuid
import java.io.IOException
import java.util.UUID

class AesKdf : KdfEngine() {

    init {
        uuid = CIPHER_UUID
    }

    override val defaultParameters: KdfParameters
        get() {
            return KdfParameters(uuid!!).apply {
                setParamUUID()
                setUInt64(PARAM_ROUNDS, UnsignedLong.from(defaultKeyRounds))
            }
        }

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray): ByteArray {

        var seed = parameters.getByteArray(PARAM_SEED)
        if (seed != null && seed.size != 32) {
            seed = HashManager.sha256(seed)
        }

        var currentMasterKey = masterKey
        if (currentMasterKey.size != 32) {
            currentMasterKey = HashManager.sha256(currentMasterKey)
        }

        val rounds = parameters.getUInt64(PARAM_ROUNDS)?.toULong()
            ?.coerceIn(minKeyRounds, maxKeyRounds)

        return AESTransformer.transformKey(seed, currentMasterKey, rounds) ?: ByteArray(0)
    }

    override fun randomize() {
        super.randomize()
        parameters.setByteArray(PARAM_SEED, HashManager.generateRandom(32))
    }

    override fun getSeed(): ByteArray? {
        return parameters.getByteArray(PARAM_SEED)
    }

    override fun getKeyRounds(): ULong {
        return parameters.getUInt64(PARAM_ROUNDS)?.toULong() ?: defaultKeyRounds
    }

    override fun setKeyRounds(keyRounds: ULong) {
        parameters.setUInt64(
            PARAM_ROUNDS,
            UnsignedLong.from(keyRounds.coerceIn(minKeyRounds, maxKeyRounds))
        )
    }

    override val defaultKeyRounds: ULong = 500_000u

    override val minKeyRounds: ULong = 1u

    override val maxKeyRounds: ULong = 100_000_000u

    override fun toString(): String {
        return "AES"
    }

    companion object {

        val CIPHER_UUID: UUID = bytes16ToUuid(
                byteArrayOf(0xC9.toByte(),
                        0xD9.toByte(),
                        0xF3.toByte(),
                        0x9A.toByte(),
                        0x62.toByte(),
                        0x8A.toByte(),
                        0x44.toByte(),
                        0x60.toByte(),
                        0xBF.toByte(),
                        0x74.toByte(),
                        0x0D.toByte(),
                        0x08.toByte(),
                        0xC1.toByte(),
                        0x8A.toByte(),
                        0x4F.toByte(),
                        0xEA.toByte()))

        const val PARAM_ROUNDS = "R" // UInt64
        const val PARAM_SEED = "S" // Byte array
    }
}
