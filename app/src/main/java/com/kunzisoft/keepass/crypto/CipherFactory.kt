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
package com.kunzisoft.keepass.crypto

import android.os.Build
import com.kunzisoft.keepass.crypto.engine.AesEngine
import com.kunzisoft.keepass.crypto.engine.ChaCha20Engine
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.engine.TwofishEngine
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.NoSuchAlgorithmException
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

object CipherFactory {

    private var blacklistInit = false
    private var blacklisted: Boolean = false

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    @JvmOverloads
    fun getInstance(transformation: String, androidOverride: Boolean = false): Cipher {
        // Return the native AES if it is possible
        return if (!deviceBlacklisted() && !androidOverride && hasNativeImplementation(transformation) && NativeLib.loaded()) {
            Cipher.getInstance(transformation, AESProvider())
        } else {
            Cipher.getInstance(transformation)
        }
    }

    fun deviceBlacklisted(): Boolean {
        if (!blacklistInit) {
            blacklistInit = true
            // The Acer Iconia A500 is special and seems to always crash in the native crypto libraries
            blacklisted = Build.MODEL == "A500"
        }
        return blacklisted
    }

    private fun hasNativeImplementation(transformation: String): Boolean {
        return transformation == "AES/CBC/PKCS5Padding"
    }

    /**
     * Generate appropriate cipher based on KeePass 2.x UUID's
     */
    @Throws(NoSuchAlgorithmException::class)
    fun getInstance(uuid: UUID): CipherEngine {
        return when (uuid) {
            AesEngine.CIPHER_UUID -> AesEngine()
            TwofishEngine.CIPHER_UUID -> TwofishEngine()
            ChaCha20Engine.CIPHER_UUID -> ChaCha20Engine()
            else -> throw NoSuchAlgorithmException("UUID unrecognized.")
        }
    }
}
