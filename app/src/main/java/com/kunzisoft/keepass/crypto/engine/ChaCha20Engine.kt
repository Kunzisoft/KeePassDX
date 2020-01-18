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

import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.stream.bytes16ToUuid
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaCha20Engine : CipherEngine() {

    override fun ivLength(): Int {
        return 12
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    override fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray, androidOverride: Boolean): Cipher {
        val cipher = Cipher.getInstance("Chacha7539", BouncyCastleProvider())
        cipher.init(opmode, SecretKeySpec(key, "ChaCha7539"), IvParameterSpec(IV))
        return cipher
    }

    override fun getPwEncryptionAlgorithm(): EncryptionAlgorithm {
        return EncryptionAlgorithm.ChaCha20
    }

    companion object {

        val CIPHER_UUID: UUID = bytes16ToUuid(
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
