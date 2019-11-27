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
package com.kunzisoft.keepass.crypto.engine

import com.kunzisoft.keepass.database.element.EncryptionAlgorithm

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

abstract class CipherEngine {

    fun keyLength(): Int {
        return 32
    }

    open fun ivLength(): Int {
        return 16
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    abstract fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray, androidOverride: Boolean): Cipher

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
        return getCipher(opmode, key, IV, false)
    }

    abstract fun getPwEncryptionAlgorithm(): EncryptionAlgorithm

}
