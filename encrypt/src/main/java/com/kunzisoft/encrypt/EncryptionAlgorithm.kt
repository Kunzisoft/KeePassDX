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
package com.kunzisoft.encrypt

import android.content.res.Resources
import com.kunzisoft.encrypt.engine.AesEngine
import com.kunzisoft.encrypt.engine.ChaCha20Engine
import com.kunzisoft.encrypt.engine.CipherEngine
import com.kunzisoft.encrypt.engine.TwofishEngine

import java.util.UUID

enum class EncryptionAlgorithm : ObjectNameResource {

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

    val dataCipher: UUID
        get() {
            return when (this) {
                AESRijndael -> AesEngine.CIPHER_UUID
                Twofish -> TwofishEngine.CIPHER_UUID
                ChaCha20 -> ChaCha20Engine.CIPHER_UUID
            }
        }

    override fun getName(resources: Resources): String {
        return when (this) {
            AESRijndael -> resources.getString(R.string.encryption_rijndael)
            Twofish -> resources.getString(R.string.encryption_twofish)
            ChaCha20 -> resources.getString(R.string.encryption_chacha20)
        }
    }
}
