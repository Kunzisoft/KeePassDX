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
package com.kunzisoft.keepass.crypto.engine


import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.stream.bytes16ToUuid
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesEngine : CipherEngine() {

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    override fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray, androidOverride: Boolean): Cipher {
        val cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding", androidOverride)
        cipher.init(opmode, SecretKeySpec(key, "AES"), IvParameterSpec(IV))
        return cipher
    }

    override fun getPwEncryptionAlgorithm(): EncryptionAlgorithm {
        return EncryptionAlgorithm.AESRijndael
    }

    companion object {

        val CIPHER_UUID: UUID = bytes16ToUuid(
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
}
