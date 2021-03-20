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
package com.kunzisoft.encrypt.keyDerivation

import android.content.res.Resources
import com.kunzisoft.encrypt.R
import com.kunzisoft.encrypt.CryptoUtil
import com.kunzisoft.encrypt.stream.bytes16ToUuid
import com.kunzisoft.encrypt.finalkey.AESKeyTransformerFactory
import java.io.IOException
import java.security.SecureRandom
import java.util.*

class AesKdf : KdfEngine() {

    init {
        uuid = CIPHER_UUID
    }

    override val defaultParameters: KdfParameters
        get() {
            return KdfParameters(uuid!!).apply {
                setParamUUID()
                setUInt64(PARAM_ROUNDS, defaultKeyRounds)
            }
        }

    override val defaultKeyRounds: Long = 500000L

    override fun getName(resources: Resources): String {
        return resources.getString(R.string.kdf_AES)
    }

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray, kdfParameters: KdfParameters): ByteArray {

        var seed = kdfParameters.getByteArray(PARAM_SEED)
        if (seed != null && seed.size != 32) {
            seed = CryptoUtil.hashSha256(seed)
        }

        var currentMasterKey = masterKey
        if (currentMasterKey.size != 32) {
            currentMasterKey = CryptoUtil.hashSha256(currentMasterKey)
        }

        val rounds = kdfParameters.getUInt64(PARAM_ROUNDS)

        return AESKeyTransformerFactory.transformMasterKey(seed, currentMasterKey, rounds) ?: ByteArray(0)
    }

    override fun randomize(kdfParameters: KdfParameters) {
        val random = SecureRandom()

        val seed = ByteArray(32)
        random.nextBytes(seed)

        kdfParameters.setByteArray(PARAM_SEED, seed)
    }

    override fun getKeyRounds(kdfParameters: KdfParameters): Long {
        return kdfParameters.getUInt64(PARAM_ROUNDS) ?: defaultKeyRounds
    }

    override fun setKeyRounds(kdfParameters: KdfParameters, keyRounds: Long) {
        kdfParameters.setUInt64(PARAM_ROUNDS, keyRounds)
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
