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

import com.kunzisoft.encrypt.CipherFactory
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

class TwofishEngine : CipherEngine() {

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    override fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
        return CipherFactory.getTwofish(opmode, key, IV, forcePaddingCompatibility)
    }

    override fun getEncryptionAlgorithm(): EncryptionAlgorithm {
        return EncryptionAlgorithm.Twofish
    }
}
