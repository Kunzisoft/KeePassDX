/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto.engine;


import com.kunzisoft.keepass.crypto.CipherFactory;
import com.kunzisoft.keepass.database.PwEncryptionAlgorithm;
import com.kunzisoft.keepass.utils.Types;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEngine extends CipherEngine {

    public static final UUID CIPHER_UUID = Types.bytestoUUID(
            new byte[]{(byte) 0x31, (byte) 0xC1, (byte) 0xF2, (byte) 0xE6, (byte) 0xBF, (byte) 0x71, (byte) 0x43, (byte) 0x50,
            (byte) 0xBE, (byte) 0x58, (byte) 0x05, (byte) 0x21, (byte) 0x6A, (byte) 0xFC, (byte)0x5A, (byte) 0xFF
    });

    @Override
    public Cipher getCipher(int opmode, byte[] key, byte[] IV, boolean androidOverride) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding", androidOverride);

        cipher.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(IV));

        return cipher;
    }

    @Override
    public PwEncryptionAlgorithm getPwEncryptionAlgorithm() {
        return PwEncryptionAlgorithm.AES_Rijndael;
    }
}
