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
 * This code is based on KeePassXC code
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/totp/totp.cpp
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/core/Entry.cpp
 */
package com.kunzisoft.keepass.totp;

import org.apache.commons.codec.binary.Base32;
import android.net.Uri;
import android.util.Patterns;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.database.PwEntry;

public class TotpSettings {

    private enum EntryType {
        None, OTP, SeedAndSettings,
    }

    private enum TokenType {
        Default, Steam
    }

    private static final int DEFAULT_STEP = 30;
    private static final int DEFAULT_DIGITS = 6;
    private static final int STEAM_DIGITS = 5;

    // Logical breakdown of key=value regex. the final string is as follows:
    // [^&=\s]+=[^&=\s]+(&[^&=\s]+=[^&=\s]+)*
    private static final String validKeyValue = "[^&=\\s]+";
    private static final String validKeyValuePair = validKeyValue + "=" + validKeyValue;
    private static final String validKeyValueRegex =
            validKeyValuePair + "&(" + validKeyValuePair + ")*";

    private static final String OTP_FIELD = "otp";
    private static final String SEED_FIELD = "TOTP Seed";
    private static final String SETTING_FIELD = "TOTP Settings";

    private PwEntry entry;
    private String seed;
    private byte[] secret;
    private int step;
    private int digits;
    private EntryType entryType;
    private TokenType tokenType;

    public TotpSettings(PwEntry entry) {
        this.entry = entry;
        if (parseOtp() || parseSeedAndSettings()) {
            secret = new Base32().decode(seed.getBytes());
        } else {
            entryType = EntryType.None;
        }
    }

    public void setSettings(String seed, int digits, int step) {
        // TODO: Implement a way to set TOTP from device
    }

    public boolean isConfigured() {
        return entryType != EntryType.None;
    }

    public String getToken() {
        if (entryType == EntryType.None) {
            return "";
        }
        switch (tokenType) {
            case Steam:
                return TotpGenerator.TOTP_Steam(secret, step, digits);
            default:
                return TotpGenerator.TOTP_RFC6238(secret, step, digits);
        }
    }

    public int getSecondsRemaining() {
        return step - (int) ((System.currentTimeMillis() / 1000) % step);
    }

    public boolean shouldRefreshToken() {
        return getSecondsRemaining() == step;
    }

    private boolean parseSeedAndSettings() {
        String seedField = getField(SEED_FIELD);
        String settingsField = getField(SETTING_FIELD);
        if (seedField == null || settingsField == null) {
            return false;
        }

        // Regex match, sync with TotpGenerator.shortNameToEncoder
        Pattern pattern = Pattern.compile("(\\d+);((?:\\d+)|S)");
        Matcher matcher = pattern.matcher(settingsField);
        if (!matcher.matches()) {
            // malformed
            return false;
        }

        step = toInt(matcher.group(1));

        String encodingType = matcher.group(2);
        digits = getDigitsForType(encodingType);

        seed = seedField;
        entryType = EntryType.SeedAndSettings;
        return true;
    }

    private boolean parseOtp() {
        String key = getField(OTP_FIELD);
        if (key == null) {
            return false;
        }

        Uri url = null;
        if (isValidUrl(key)) {
            url = Uri.parse(key);
        }
        boolean useEncoder = false;

        if (url != null && url.getScheme().equals("otpauth")) {
            // Default OTP url format

            seed = url.getQueryParameter("secret");
            digits = toInt(url.getQueryParameter("digits"));
            step = toInt(url.getQueryParameter("period"));

            String encName = url.getQueryParameter("encoder");
            digits = getDigitsForType(encName);
        } else if (Pattern.matches(validKeyValueRegex, key)) {
            // KeeOtp string format
            HashMap<String, String> query = breakDownKeyValuePairs(key);

            seed = query.get("key");
            digits = toInt(query.get("size"));
            step = toInt(query.get("step"));
        } else {
            // Malformed
            return false;
        }

        if (digits == 0) {
            digits = DEFAULT_DIGITS;
        }

        if (step <= 0 || step > 60) {
            step = DEFAULT_STEP;
        }

        entryType = EntryType.OTP;
        return true;
    }

    private String getField(String id) {
        ProtectedString field = entry.getFields().getListOfAllFields().get(id);
        if (field != null) {
            return field.toString();
        }
        return null;
    }

    private boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    private int toInt(String value) {
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
        HashMap<String, String> output = new HashMap<String, String>();
        for (String element : elements) {
            String[] pair = element.split("=");
            output.put(pair[0], pair[1]);
        }
        return output;
    }

    private int getDigitsForType(String encodingType) {
        int digitType = toInt(encodingType);
        if (digitType != 0) {
            tokenType = TokenType.Default;
            return digitType;
        }
        switch (encodingType) {
            case "S":
            case "steam":
                tokenType = TokenType.Steam;
                return 5;
            default:
                tokenType = TokenType.Default;
                return DEFAULT_DIGITS;
        }
    }
}
