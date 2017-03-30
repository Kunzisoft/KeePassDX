/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.crypto.engine;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.utils.Types;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TwofishEngine extends CipherEngine {
    public static final UUID CIPHER_UUID = Types.bytestoUUID(
            new byte[]{(byte)0xAD, (byte)0x68, (byte)0xF2, (byte)0x9F, (byte)0x57, (byte)0x6F, (byte)0x4B, (byte)0xB9,
                    (byte)0xA3, (byte)0x6A, (byte)0xD4, (byte)0x7A, (byte)0xF9, (byte)0x65, (byte)0x34, (byte)0x6C
            });
    @Override
    public Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher;
        if (opmode == Cipher.ENCRYPT_MODE) {
            cipher = CipherFactory.getInstance("Twofish/CBC/ZeroBytePadding", androidOverride);
        } else {
            cipher = CipherFactory.getInstance("Twofish/CBC/NoPadding", androidOverride);
        }

        cipher.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(IV));

        return cipher;
    }
}
