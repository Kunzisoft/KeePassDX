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
package com.keepassdroid.crypto;

import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.stream.NullOutputStream;

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;

public class CryptoUtil {
    public static byte[] resizeKey(byte[] in, int inOffset, int cbIn, int cbOut) {
        if (cbOut == 0) return new byte[0];

        byte[] hash;
        if (cbOut <= 32) { hash = hashSha256(in, inOffset, cbIn); }
        else { hash = hashSha512(in, inOffset, cbIn); }

        if (cbOut == hash.length) { return hash; }

        byte[] ret = new byte[cbOut];
        if (cbOut < hash.length) {
            System.arraycopy(hash, 0, ret, 0, cbOut);
        }
        else {
            int pos = 0;
            long r = 0;
            while (pos < cbOut) {
                Mac hmac;
                try {
                    hmac = Mac.getInstance("HmacSHA256");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                byte[] pbR = LEDataOutputStream.writeLongBuf(r);
                byte[] part = hmac.doFinal(pbR);

                int copy = Math.min(cbOut - pos, part.length);
                assert(copy > 0);

                System.arraycopy(part, 0, ret, pos, copy);
                pos += copy;
                r++;

                Arrays.fill(part, (byte)0);
            }
            assert(pos == cbOut);
        }

        Arrays.fill(hash, (byte)0);
        return ret;
    }

    public static byte[] hashSha256(byte[] data) {
        return hashSha256(data, 0, data.length);
    }

    public static byte[] hashSha256(byte[] data, int offset, int count) {
        return hashGen("SHA-256", data, offset, count);
    }

    public static byte[] hashSha512(byte[] data) {
        return hashSha512(data, 0, data.length);
    }

    public static byte[] hashSha512(byte[] data, int offset, int count) {
        return hashGen("SHA-512", data, offset, count);
    }

    public static byte[] hashGen(String transform, byte[] data, int offset, int count) {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance(transform);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        NullOutputStream nos = new NullOutputStream();
        DigestOutputStream dos = new DigestOutputStream(nos, hash);

        try {
            dos.write(data, offset, count);
            dos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return hash.digest();
    }
}
