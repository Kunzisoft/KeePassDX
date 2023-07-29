/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 * 
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with KeePassDX. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * This code is based on andOTP code
 * https://github.com/andOTP/andOTP/blob/master/app/src/main/java/org/shadowice/flocke/andotp/
 * Utilities/TokenCalculator.java
 */
package com.kunzisoft.keepass.otp;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TokenCalculator {
    public static final int TOTP_DEFAULT_PERIOD = 30;
    public static final long HOTP_INITIAL_COUNTER = 1;
    public static final int OTP_DEFAULT_DIGITS = 6;
    public static final int STEAM_DEFAULT_DIGITS = 5;
    public static final HashAlgorithm OTP_DEFAULT_ALGORITHM = HashAlgorithm.SHA1;

    private static final char[] STEAMCHARS = new char[] {
            '2', '3', '4', '5', '6', '7', '8', '9', 'B', 'C',
            'D', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
            'R', 'T', 'V', 'W', 'X', 'Y'
    };

    public enum HashAlgorithm {
        SHA1, SHA256, SHA512;

        static HashAlgorithm fromString(String hashString) {
            String hash = hashString.replace("[^a-zA-Z0-9]", "").toUpperCase();
            try {
                return valueOf(hash);
            } catch (Exception e) {
                return OTP_DEFAULT_ALGORITHM;
            }
        }
    }

    private static byte[] generateHash(HashAlgorithm algorithm, byte[] key, byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String algo = "Hmac" + algorithm.toString();

        Mac mac = Mac.getInstance(algo);
        mac.init(new SecretKeySpec(key, algo));

        return mac.doFinal(data);
    }

    public static int TOTP_RFC6238(byte[] secret, int period, long time, int digits, HashAlgorithm algorithm) {
        int fullToken = TOTP(secret, period, time, algorithm);
        int div = (int) Math.pow(10, digits);

        return fullToken % div;
    }

    public static String TOTP_RFC6238(byte[] secret, int period, int digits, HashAlgorithm algorithm) {
        return formatTokenString(TOTP_RFC6238(secret, period, System.currentTimeMillis() / 1000, digits, algorithm), digits);
    }

    public static String TOTP_Steam(byte[] secret, int period, int digits, HashAlgorithm algorithm) {
        int fullToken = TOTP(secret, period, System.currentTimeMillis() / 1000, algorithm);

        StringBuilder tokenBuilder = new StringBuilder();

        for (int i = 0; i < digits; i++) {
            tokenBuilder.append(STEAMCHARS[fullToken % STEAMCHARS.length]);
            fullToken /= STEAMCHARS.length;
        }

        return tokenBuilder.toString();
    }

    public static String HOTP(byte[] secret, long counter, int digits, HashAlgorithm algorithm) {
        int fullToken = HOTP(secret, counter, algorithm);
        int div = (int) Math.pow(10, digits);

        return formatTokenString(fullToken % div, digits);
    }

    private static int TOTP(byte[] key, int period, long time, HashAlgorithm algorithm) {
        return HOTP(key, time / period, algorithm);
    }

    private static int HOTP(byte[] key, long counter, HashAlgorithm algorithm) {
        int r = 0;

        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            byte[] hash = generateHash(algorithm, key, data);

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

    public static String formatTokenString(int token, int digits) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        numberFormat.setMinimumIntegerDigits(digits);
        numberFormat.setGroupingUsed(false);

        return numberFormat.format(token);
    }
}