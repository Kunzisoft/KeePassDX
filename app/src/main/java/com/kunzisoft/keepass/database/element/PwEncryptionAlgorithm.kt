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
package com.kunzisoft.keepass.database.element

import android.content.res.Resources

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.engine.AesEngine
import com.kunzisoft.keepass.crypto.engine.ChaCha20Engine
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.engine.TwofishEngine
import com.kunzisoft.keepass.database.ObjectNameResource

import java.util.UUID

enum class PwEncryptionAlgorithm : ObjectNameResource {

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
