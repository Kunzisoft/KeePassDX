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

import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.database.element.PwEncryptionAlgorithm
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.UUID

import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class TwofishEngine : CipherEngine() {

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    override fun getCipher(opmode: Int, key: ByteArray, IV: ByteArray, androidOverride: Boolean): Cipher {
        val cipher: Cipher = if (opmode == Cipher.ENCRYPT_MODE) {
            CipherFactory.getInstance("Twofish/CBC/ZeroBytePadding", androidOverride)
        } else {
            CipherFactory.getInstance("Twofish/CBC/NoPadding", androidOverride)
        }
        cipher.init(opmode, SecretKeySpec(key, "AES"), IvParameterSpec(IV))

        return cipher
    }

    override fun getPwEncryptionAlgorithm(): PwEncryptionAlgorithm {
        return PwEncryptionAlgorithm.Twofish
    }

    companion object {

        val CIPHER_UUID: UUID = DatabaseInputOutputUtils.bytesToUuid(
                byteArrayOf(0xAD.toByte(), 0x68.toByte(), 0xF2.toByte(), 0x9F.toByte(), 0x57.toByte(), 0x6F.toByte(), 0x4B.toByte(), 0xB9.toByte(), 0xA3.toByte(), 0x6A.toByte(), 0xD4.toByte(), 0x7A.toByte(), 0xF9.toByte(), 0x65.toByte(), 0x34.toByte(), 0x6C.toByte()))
    }
}
