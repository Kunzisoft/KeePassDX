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

import android.os.Build
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

object CipherFactory {

    private var blacklistInit = false
    private var blacklisted: Boolean = false

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
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

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun getInstance(transformation: String, androidOverride: Boolean = false): Cipher {
        // Return the native AES if it is possible
        return if (!deviceBlacklisted() && !androidOverride && hasNativeImplementation(transformation) && NativeLib.loaded()) {
            Cipher.getInstance(transformation, AESProvider())
        } else {
            Cipher.getInstance(transformation)
        }
    }
}
