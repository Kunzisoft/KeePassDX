/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 * This code is based on KeePassXC code
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/totp/totp.cpp
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/core/Entry.cpp
 */
package com.kunzisoft.keepass.otp;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kunzisoft.keepass.database.element.EntryVersioned;
import com.kunzisoft.keepass.database.element.security.ProtectedString;

public class OtpEntryFields {

    private static final String TAG = OtpEntryFields.class.getName();

    // Field from KeePassXC
    private static final String OTP_FIELD = "otp";

    // URL parameters (https://github.com/google/google-authenticator/wiki/Key-Uri-Format)
    private static final String OTP_SCHEME = "otpauth";
    private static final String TOTP_AUTHORITY = "totp"; // time-based
    private static final String HOTP_AUTHORITY = "hotp"; // counter-based
    private static final String ISSUER_URL_PARAM = "issuer";
    private static final String SECRET_URL_PARAM = "secret";
    private static final String DIGITS_URL_PARAM = "digits";
    private static final String PERIOD_URL_PARAM = "period";
    private static final String ENCODER_URL_PARAM = "encoder";
    private static final String COUNTER_URL_PARAM = "counter";

    // Key-values (maybe from plugin or old KeePassXC)
    private static final String SEED_KEY = "key";
    private static final String DIGITS_KEY = "size";
    private static final String STEP_KEY = "step";

    // HmacOtp KeePass2 values (https://keepass.info/help/base/placeholders.html#hmacotp)
    private static final String HMACOTP_SECRET_KEY = "HmacOtp-Secret";
    private static final String HMACOTP_SECRET_HEX_KEY = "HmacOtp-Secret-Hex";
    private static final String HMACOTP_SECRET_BASE32_KEY = "HmacOtp-Secret-Base32";
    private static final String HMACOTP_SECRET_BASE64_KEY = "HmacOtp-Secret-Base64";
    private static final String HMACOTP_SECRET_COUNTER_KEY = "HmacOtp-Counter";

    // Custom fields (maybe from plugin)
    private static final String TOTP_SEED_FIELD = "TOTP Seed";
    private static final String TOTP_SETTING_FIELD = "TOTP Settings";

    public enum OtpType {
        UNDEFINED,
        HOTP,   // counter based
        TOTP    // time based
    }

    private enum TokenType {
        Default(6),
        Steam(5);

        public int digits;

        TokenType(int digits) {
            this.digits = digits;
        }

        public static TokenType getFromString(@Nullable String tokenType) {
            if (tokenType == null)
                return Default;
            switch (tokenType.toLowerCase()) {
                case "s":
                case "steam":
                    return Steam;
                default:
                    return Default;
            }
        }
    }
    // Default values
    private static final int DEFAULT_HOTP_COUNTER = 0;
    private static final int DEFAULT_STEP = 30;

    // Logical breakdown of key=value regex. the final string is as follows:
    // [^&=\s]+=[^&=\s]+(&[^&=\s]+=[^&=\s]+)*
    private static final String validKeyValue = "[^&=\\s]+";
    private static final String validKeyValuePair = validKeyValue + "=" + validKeyValue;
    private static final String validKeyValueRegex =
            validKeyValuePair + "&(" + validKeyValuePair + ")*";

    private EntryVersioned entry;

    private OtpType type = OtpType.UNDEFINED; // ie : HOTP or TOTP
    private TokenType tokenType = TokenType.Default; // ie : default or Steam
    private String name = ""; // ie : user@email.com
    private String issuer = ""; // ie : Gitlab
    private byte[] secret;
    private int counter = DEFAULT_HOTP_COUNTER; // ie : 5 - only for HOTP
    private int step = DEFAULT_STEP; // ie : 30 seconds - only for TOTP
    private int digits = TokenType.Default.digits; // ie : 6 - number of digits generated

    public OtpEntryFields(EntryVersioned entry) {
        this.entry = entry;

        // OTP (HOTP/TOTP) from URL and field from KeePassXC
        boolean parse = parseOtpUri();
        // TOTP from key values (maybe plugin or old KeePassXC)
        if (!parse)
            parse = parseTotpKeyValues();
        // TOTP from custom field
        if (!parse)
            parse = parseTOTPFromField();
        // HOTP fields from KeePass 2
        if (!parse)
            parseHOTPFromField();
    }

    public OtpType getType() {
        return type;
    }

    public String getToken() {
        switch (type) {
            case HOTP:
                return OtpTokenGenerator.HOTP(secret, counter, digits);
            case TOTP:
                switch (tokenType) {
                    case Steam:
                        return OtpTokenGenerator.TOTP_Steam(secret, step, digits);
                    case Default:
                    default:
                        return OtpTokenGenerator.TOTP_RFC6238(secret, step, digits);
                }
            case UNDEFINED:
            default:
                return "";
        }
    }

    public int getSecondsRemaining() {
        return step - (int) ((System.currentTimeMillis() / 1000) % step);
    }

    public boolean shouldRefreshToken() {
        return getSecondsRemaining() == step;
    }

    public void setSettings(@NonNull String seed, int digits, int step) {
        // TODO: Implement a way to set TOTP from device
    }

    private void setName(@NonNull String name) {
        this.name = name;
    }

    private void setIssuer(@NonNull String issuer) {
        this.issuer = issuer;
    }

    private void setBase32Secret(@NonNull String secret) {
        this.secret = new Base32().decode(secret.getBytes());
    }

    private void setBase64Secret(@NonNull String secret) {
        this.secret = new Base64().decode(secret.getBytes());
    }

    private void setCounter(int counter) {
        if (counter < 0) {
            this.counter = counter;
        } else {
            this.counter = counter;
        }
    }

    public int getStep() {
        return step;
    }

    private void setStep(int step) {
        if (step <= 0 || step > 60) {
            this.step = DEFAULT_STEP;
        } else {
            this.step = step;
        }
    }

    private void setDigits(int digits) {
        if (digits <= 0) {
            this.digits = TokenType.Default.digits;
        } else {
            this.digits = digits;
        }
    }

    /**
     * Parses a secret value from a URI. The format will be:
     *
     * <p>otpauth://totp/user@example.com?secret=FFF...
     *
     * <p>otpauth://hotp/user@example.com?secret=FFF...&counter=123
     */
    private boolean parseOtpUri() {
        String otpPlainText = getField(OTP_FIELD);
        if (otpPlainText != null
                && !otpPlainText.isEmpty()) {
            Uri uri = Uri.parse(otpPlainText);

            if (uri.getScheme() == null
                    || !OTP_SCHEME.equals(uri.getScheme().toLowerCase())) {
                Log.e(TAG, "Invalid or missing scheme in uri");
                return false;
            }

            final String authority = uri.getAuthority();
            if (TOTP_AUTHORITY.equals(authority)) {
                type = OtpType.TOTP;

            } else if (HOTP_AUTHORITY.equals(authority)) {
                type = OtpType.HOTP;

                String counterParameter = uri.getQueryParameter(COUNTER_URL_PARAM);
                if (counterParameter != null) {
                    try {
                        setCounter(Integer.parseInt(counterParameter));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid counter in uri");
                        return false;
                    }
                }

            } else {
                Log.e(TAG, "Invalid or missing authority in uri");
                return false;
            }

            String nameParam = validateAndGetNameInPath(uri.getPath());
            if (nameParam != null && !nameParam.isEmpty())
                setName(nameParam);

            String issuerParam = uri.getQueryParameter(ISSUER_URL_PARAM);
            if (issuerParam != null && !issuerParam.isEmpty())
                setIssuer(issuerParam);

            String secretParam = uri.getQueryParameter(SECRET_URL_PARAM);
            if (secretParam != null && !secretParam.isEmpty())
                setBase32Secret(secretParam);

            String encoderParam = uri.getQueryParameter(ENCODER_URL_PARAM);
            if (encoderParam != null && !encoderParam.isEmpty()) {
                tokenType = TokenType.getFromString(encoderParam);
                setDigits(tokenType.digits);
            }

            String digitsParam = uri.getQueryParameter(DIGITS_URL_PARAM);
            if (digitsParam != null && !digitsParam.isEmpty())
                setDigits(toInt(digitsParam));

            String counterParam = uri.getQueryParameter(COUNTER_URL_PARAM);
            if (counterParam != null && !counterParam.isEmpty())
                setCounter(toInt(counterParam));

            String stepParam = uri.getQueryParameter(PERIOD_URL_PARAM);
            if (stepParam != null && !stepParam.isEmpty())
                setStep(toInt(stepParam));

            return true;
        }
        return false;
    }

    private static String validateAndGetNameInPath(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        // path is "/name", so remove leading "/", and trailing white spaces
        String name = path.substring(1).trim();
        if (name.length() == 0) {
            return null; // only white spaces.
        }
        return name;
    }

    private boolean parseTotpKeyValues() {
        String plainText = getField(OTP_FIELD);
        if (plainText != null
                && !plainText.isEmpty()) {
            if (Pattern.matches(validKeyValueRegex, plainText)) {
                // KeeOtp string format
                HashMap<String, String> query = breakDownKeyValuePairs(plainText);

                String secretString = query.get(SEED_KEY);
                if (secretString == null)
                    secretString = "";
                setBase32Secret(secretString);
                setDigits(toInt(query.get(DIGITS_KEY)));
                setStep(toInt(query.get(STEP_KEY)));

                type = OtpType.TOTP;
                return true;
            } else {
                // Malformed
                return false;
            }
        }
        return false;
    }

    private boolean parseTOTPFromField() {
        String seedField = getField(TOTP_SEED_FIELD);
        if (seedField == null) {
            return false;
        }
        setBase32Secret(seedField);

        String settingsField = getField(TOTP_SETTING_FIELD);
        if (settingsField != null) {
            // Regex match, sync with OtpTokenGenerator.shortNameToEncoder
            Pattern pattern = Pattern.compile("(\\d+);((?:\\d+)|S)");
            Matcher matcher = pattern.matcher(settingsField);
            if (!matcher.matches()) {
                // malformed
                return false;
            }
            setStep(toInt(matcher.group(1)));
            setDigits(TokenType.getFromString(matcher.group(2)).digits);
        }

        type = OtpType.TOTP;
        return true;
    }

    private boolean parseHOTPFromField() {
        String secretField = getField(HMACOTP_SECRET_KEY);
        String secretHexField = getField(HMACOTP_SECRET_HEX_KEY);
        String secretBase32Field = getField(HMACOTP_SECRET_BASE32_KEY);
        String secretBase64Field = getField(HMACOTP_SECRET_BASE64_KEY);
        if (secretField != null)
            secret = secretField.getBytes(Charset.forName("UTF-8"));
        else if (secretHexField != null) {
            try {
                secret = Hex.decodeHex(secretHexField);
            } catch (DecoderException e) {
                e.printStackTrace();
                return false;
            }
        }
        else if (secretBase32Field != null)
            setBase32Secret(secretBase32Field);
        else if (secretBase64Field != null)
            setBase64Secret(secretBase64Field);
        else
            return false;

        String secretCounterField = getField(HMACOTP_SECRET_COUNTER_KEY);
        if (secretCounterField != null) {
            setCounter(toInt(secretCounterField));
        }

        type = OtpType.HOTP;
        return true;
    }

    private String getField(String id) {
        ProtectedString field = entry.getCustomFields().get(id);
        if (field != null) {
            return field.toString();
        }
        return null;
    }

    private static int toInt(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private HashMap<String, String> breakDownKeyValuePairs(String pairs) {
        String[] elements = pairs.split("&");
        HashMap<String, String> output = new HashMap<>();
        for (String element : elements) {
            String[] pair = element.split("=");
            output.put(pair[0], pair[1]);
        }
        return output;
    }
}
