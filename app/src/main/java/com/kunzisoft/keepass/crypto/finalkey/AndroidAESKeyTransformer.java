/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.crypto.finalkey;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class AndroidAESKeyTransformer extends KeyTransformer {

    @Override
    public byte[] transformMasterKey(byte[] pKeySeed, byte[] pKey, long rounds) throws IOException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("NoSuchAlgorithm: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new IOException("NoSuchPadding: " + e.getMessage());
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(pKeySeed, "AES"));
        } catch (InvalidKeyException e) {
            throw new IOException("InvalidPasswordException: " + e.getMessage());
        }

        // Encrypt key rounds times
        byte[] newKey = new byte[pKey.length];
        System.arraycopy(pKey, 0, newKey, 0, pKey.length);
        byte[] destKey = new byte[pKey.length];
        for (int i = 0; i < rounds; i++) {
            try {
                cipher.update(newKey, 0, newKey.length, destKey, 0);
                System.arraycopy(destKey, 0, newKey, 0, newKey.length);

            } catch (ShortBufferException e) {
                throw new IOException("Short buffer: " + e.getMessage());
            }
        }

        // Hash the key
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            assert true;
            throw new IOException("SHA-256 not implemented here: " + e.getMessage());
        }

        md.update(newKey);
        return md.digest();
    }

}
