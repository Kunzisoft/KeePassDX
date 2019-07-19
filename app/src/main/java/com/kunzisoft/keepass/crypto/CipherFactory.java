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
package com.kunzisoft.keepass.crypto;

import android.os.Build;

import com.kunzisoft.keepass.crypto.engine.AesEngine;
import com.kunzisoft.keepass.crypto.engine.ChaCha20Engine;
import com.kunzisoft.keepass.crypto.engine.CipherEngine;
import com.kunzisoft.keepass.crypto.engine.TwofishEngine;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class CipherFactory {
    private static boolean blacklistInit = false;
    private static boolean blacklisted;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static Cipher getInstance(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
        return getInstance(transformation, false);
    }

    public static Cipher getInstance(String transformation, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException {
        // Return the native AES if it is possible
        if ( (!deviceBlacklisted()) && (!androidOverride) && hasNativeImplementation(transformation) && NativeLib.INSTANCE.loaded() ) {
            return Cipher.getInstance(transformation, new AESProvider());
        } else {
            return Cipher.getInstance(transformation);
        }
    }

    public static boolean deviceBlacklisted() {
        if (!blacklistInit) {
            blacklistInit = true;

            // The Acer Iconia A500 is special and seems to always crash in the native crypto libraries
            blacklisted = Build.MODEL.equals("A500");
        }
        return blacklisted;
    }

    private static boolean hasNativeImplementation(String transformation) {
        return transformation.equals("AES/CBC/PKCS5Padding");
    }


    /** Generate appropriate cipher based on KeePass 2.x UUID's
     * @param uuid
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */
    public static CipherEngine getInstance(UUID uuid) throws NoSuchAlgorithmException {
        if ( uuid.equals(AesEngine.Companion.getCIPHER_UUID()) ) {
            return new AesEngine();
        } else if ( uuid.equals(TwofishEngine.Companion.getCIPHER_UUID()) ) {
            return new TwofishEngine();
        } else if ( uuid.equals(ChaCha20Engine.Companion.getCIPHER_UUID())) {
            return new ChaCha20Engine();
        }
        throw new NoSuchAlgorithmException("UUID unrecognized.");
    }
}
