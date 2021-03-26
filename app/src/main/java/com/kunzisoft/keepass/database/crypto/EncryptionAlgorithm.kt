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
package com.kunzisoft.keepass.database.crypto

import com.kunzisoft.keepass.utils.bytes16ToUuid
import java.security.NoSuchAlgorithmException
import java.util.*

enum class EncryptionAlgorithm {

    AESRijndael,
    Twofish,
    ChaCha20;

    val cipherEngine: CipherEngine
        get() {
            return when (this) {
                AESRijndael -> AesEngine()
                Twofish -> TwofishEngine()
                ChaCha20 -> ChaCha20Engine()
            }
        }

    val uuid: UUID
        get() {
            return when (this) {
                AESRijndael -> AES_UUID
                Twofish -> TWOFISH_UUID
                ChaCha20 -> CHACHA20_UUID
            }
        }

    override fun toString(): String {
        return when (this) {
            AESRijndael -> "Rijndael (AES)"
            Twofish -> "Twofish"
            ChaCha20 -> "ChaCha20"
        }
    }

    companion object {

        /**
         * Generate appropriate cipher based on KeePass 2.x UUID's
         */
        @Throws(NoSuchAlgorithmException::class)
        fun getFrom(uuid: UUID): EncryptionAlgorithm {
            return when (uuid) {
                AES_UUID -> AESRijndael
                TWOFISH_UUID -> Twofish
                CHACHA20_UUID -> ChaCha20
                else -> throw NoSuchAlgorithmException("UUID unrecognized.")
            }
        }

        private val AES_UUID: UUID by lazy {
            bytes16ToUuid(
                    byteArrayOf(0x31.toByte(),
                            0xC1.toByte(),
                            0xF2.toByte(),
                            0xE6.toByte(),
                            0xBF.toByte(),
                            0x71.toByte(),
                            0x43.toByte(),
                            0x50.toByte(),
                            0xBE.toByte(),
                            0x58.toByte(),
                            0x05.toByte(),
                            0x21.toByte(),
                            0x6A.toByte(),
                            0xFC.toByte(),
                            0x5A.toByte(),
                            0xFF.toByte()))
        }

        private val TWOFISH_UUID: UUID by lazy {
            bytes16ToUuid(
                    byteArrayOf(0xAD.toByte(),
                            0x68.toByte(),
                            0xF2.toByte(),
                            0x9F.toByte(),
                            0x57.toByte(),
                            0x6F.toByte(),
                            0x4B.toByte(),
                            0xB9.toByte(),
                            0xA3.toByte(),
                            0x6A.toByte(),
                            0xD4.toByte(),
                            0x7A.toByte(),
                            0xF9.toByte(),
                            0x65.toByte(),
                            0x34.toByte(),
                            0x6C.toByte()))
        }

        private val CHACHA20_UUID: UUID by lazy {
            bytes16ToUuid(
                    byteArrayOf(0xD6.toByte(),
                            0x03.toByte(),
                            0x8A.toByte(),
                            0x2B.toByte(),
                            0x8B.toByte(),
                            0x6F.toByte(),
                            0x4C.toByte(),
                            0xB5.toByte(),
                            0xA5.toByte(),
                            0x24.toByte(),
                            0x33.toByte(),
                            0x9A.toByte(),
                            0x31.toByte(),
                            0xDB.toByte(),
                            0xB5.toByte(),
                            0x9A.toByte()))
        }
    }
}
