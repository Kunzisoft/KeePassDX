/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 * 
 * This file is part of KeePass DX.
 *
 * KeePass DX is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * KeePass DX is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with KeePass DX. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * This code is based on andOTP code
 * https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/
 * Utilities/TokenCalculator.java
 */
package com.kunzisoft.keepass.totp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import android.net.Uri;
import android.util.Patterns;

public final class TotpGenerator {

    private static final char[] STEAM_CHARS =
            new char[] {'2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C', 'D', 'F', 'G', 'H', 'J',
                    'K', 'M', 'N', 'P', 'Q', 'R', 'T', 'V', 'W', 'X', 'Y'};
    private static final String ALGORITHM = "HmacSHA1";

    private static byte[] generateHash(byte[] key, byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));

        return mac.doFinal(data);
    }

    public static int TOTP_RFC6238(byte[] secret, int period, long time, int digits) {
        int fullToken = TOTP(secret, period, time);
        int div = (int) Math.pow(10, digits);

        return fullToken % div;
    }

    public static String TOTP_RFC6238(byte[] secret, int period, int digits) {
        int token = TOTP_RFC6238(secret, period, System.currentTimeMillis() / 1000, digits);

        return String.format("%0" + digits + "d", token);
    }

    public static String TOTP_Steam(byte[] secret, int period, int digits) {
        int fullToken = TOTP(secret, period, System.currentTimeMillis() / 1000);

        StringBuilder tokenBuilder = new StringBuilder();

        for (int i = 0; i < digits; i++) {
            tokenBuilder.append(STEAM_CHARS[fullToken % STEAM_CHARS.length]);
            fullToken /= STEAM_CHARS.length;
        }

        return tokenBuilder.toString();
    }

    public static String HOTP(byte[] secret, long counter, int digits) {
        int fullToken = HOTP(secret, counter);
        int div = (int) Math.pow(10, digits);

        return String.format("%0" + digits + "d", fullToken % div);
    }

    private static int TOTP(byte[] key, int period, long time) {
        return HOTP(key, time / period);
    }

    private static int HOTP(byte[] key, long counter) {
        int r = 0;

        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            byte[] hash = generateHash(key, data);

            int offset = hash[hash.length - 1] & 0xF;

            int binary = (hash[offset] & 0x7F) << 0x18;
            binary |= (hash[offset + 1] & 0xFF) << 0x10;
            binary |= (hash[offset + 2] & 0xFF) << 0x08;
            binary |= (hash[offset + 3] & 0xFF);

            r = binary;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return r;
    }

}
