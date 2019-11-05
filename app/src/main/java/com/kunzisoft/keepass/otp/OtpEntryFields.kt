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
package com.kunzisoft.keepass.otp

import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.otp.TokenCalculator.*
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

class OtpEntryFields(private val entry: EntryVersioned) {

    var type = OtpType.UNDEFINED
        private set // ie : HOTP or TOTP
    var name = "" // ie : user@email.com
    var issuer = "" // ie : Gitlab
    var secret: ByteArray? = null
        private set
    var counter = HOTP_INITIAL_COUNTER // ie : 5 - only for HOTP
        set(value) {
            field = if (value < 0) HOTP_INITIAL_COUNTER else value
        }
    var step = TOTP_DEFAULT_PERIOD
        private set(step) = if (step <= 0 || step > 60) {
            field = TOTP_DEFAULT_PERIOD
        } else {
            field = step
        } // ie : 30 seconds - only for TOTP
    var digits = TokenType.Default.tokenDigits // ie : 6 - number of digits generated
        set(value) {
            field = if (value <= 0) TokenType.Default.tokenDigits else value
        }
    var otpAlgorithm: HashAlgorithm = DEFAULT_ALGORITHM

    val token: String
        get() {
            return when (type) {
                OtpType.HOTP -> HOTP(secret, counter.toLong(), digits, otpAlgorithm)
                OtpType.TOTP -> when (tokenType) {
                    TokenType.Steam -> TOTP_Steam(secret, this.step, digits, otpAlgorithm)
                    TokenType.Default -> TOTP_RFC6238(secret, this.step, digits, otpAlgorithm)
                }
                OtpType.UNDEFINED -> ""
            }
        }

    val secondsRemaining: Int
        get() = this.step - (System.currentTimeMillis() / 1000 % this.step).toInt()

    enum class OtpType {
        UNDEFINED,
        HOTP,   // counter based
        TOTP    // time based
    }

    private var tokenType = TokenType.Default // ie : default or Steam
    private enum class TokenType constructor(var tokenDigits: Int) {
        Default(TOTP_DEFAULT_DIGITS),
        Steam(STEAM_DEFAULT_DIGITS);

        companion object {
            fun getFromString(tokenType: String?): TokenType {
                if (tokenType == null)
                    return Default
                return when (tokenType) {
                    "S", "steam" -> Steam
                    else -> Default
                }
            }
        }
    }

    init {
        // OTP (HOTP/TOTP) from URL and field from KeePassXC
        var parse = parseOtpUri()
        // TOTP from key values (maybe plugin or old KeePassXC)
        if (!parse)
            parse = parseTotpKeyValues()
        // TOTP from custom field
        if (!parse)
            parse = parseTOTPFromField()
        // HOTP fields from KeePass 2
        if (!parse)
            parseHOTPFromField()
    }

    fun shouldRefreshToken(): Boolean {
        return secondsRemaining == this.step
    }

    fun setSettings(seed: String, digits: Int, step: Int) {
        // TODO: Implement a way to set TOTP from device
    }

    private fun setBase32Secret(secret: String) {
        this.secret = Base32().decode(secret.toByteArray())
    }

    private fun setBase64Secret(secret: String) {
        this.secret = Base64().decode(secret.toByteArray())
    }

    /**
     * Parses a secret value from a URI. The format will be:
     *
     *
     * otpauth://totp/user@example.com?secret=FFF...
     *
     *
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     */
    private fun parseOtpUri(): Boolean {
        val otpPlainText = getField(OTP_FIELD)
        if (otpPlainText != null && !otpPlainText.isEmpty()) {
            val uri = Uri.parse(otpPlainText)

            if (uri.scheme == null || OTP_SCHEME != uri.scheme!!.toLowerCase()) {
                Log.e(TAG, "Invalid or missing scheme in uri")
                return false
            }

            val authority = uri.authority
            if (TOTP_AUTHORITY == authority) {
                type = OtpType.TOTP

            } else if (HOTP_AUTHORITY == authority) {
                type = OtpType.HOTP

                val counterParameter = uri.getQueryParameter(COUNTER_URL_PARAM)
                if (counterParameter != null) {
                    try {
                        counter = Integer.parseInt(counterParameter)
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "Invalid counter in uri")
                        return false
                    }

                }

            } else {
                Log.e(TAG, "Invalid or missing authority in uri")
                return false
            }

            val nameParam = validateAndGetNameInPath(uri.path)
            if (nameParam != null && nameParam.isNotEmpty())
                name = nameParam

            val algorithmParam = uri.getQueryParameter(ALGORITHM_URL_PARAM)
            if (algorithmParam != null && algorithmParam.isNotEmpty())
                otpAlgorithm = HashAlgorithm.valueOf(algorithmParam.toUpperCase(Locale.ENGLISH))

            val issuerParam = uri.getQueryParameter(ISSUER_URL_PARAM)
            if (issuerParam != null && issuerParam.isNotEmpty())
                issuer = issuerParam

            val secretParam = uri.getQueryParameter(SECRET_URL_PARAM)
            if (secretParam != null && secretParam.isNotEmpty())
                setBase32Secret(secretParam)

            val encoderParam = uri.getQueryParameter(ENCODER_URL_PARAM)
            if (encoderParam != null && encoderParam.isNotEmpty()) {
                tokenType = TokenType.getFromString(encoderParam)
                digits = tokenType.tokenDigits
            }

            val digitsParam = uri.getQueryParameter(DIGITS_URL_PARAM)
            if (digitsParam != null && digitsParam.isNotEmpty())
                digits = digitsParam.toInt()

            val counterParam = uri.getQueryParameter(COUNTER_URL_PARAM)
            if (counterParam != null && counterParam.isNotEmpty())
                counter = counterParam.toInt()

            val stepParam = uri.getQueryParameter(PERIOD_URL_PARAM)
            if (stepParam != null && stepParam.isNotEmpty())
                step = stepParam.toInt()

            return true
        }
        return false
    }

    private fun parseTotpKeyValues(): Boolean {
        val plainText = getField(OTP_FIELD)
        if (plainText != null && plainText.isNotEmpty()) {
            if (Pattern.matches(validKeyValueRegex, plainText)) {
                // KeeOtp string format
                val query = breakDownKeyValuePairs(plainText)

                var secretString = query[SEED_KEY]
                if (secretString == null)
                    secretString = ""
                setBase32Secret(secretString)
                digits = query[DIGITS_KEY]?.toInt() ?: TOTP_DEFAULT_DIGITS
                step = query[STEP_KEY]?.toInt() ?: TOTP_DEFAULT_PERIOD

                type = OtpType.TOTP
                return true
            } else {
                // Malformed
                return false
            }
        }
        return false
    }

    private fun parseTOTPFromField(): Boolean {
        val seedField = getField(TOTP_SEED_FIELD) ?: return false
        setBase32Secret(seedField)

        val settingsField = getField(TOTP_SETTING_FIELD)
        if (settingsField != null) {
            // Regex match, sync with shortNameToEncoder
            val pattern = Pattern.compile("(\\d+);((?:\\d+)|S)")
            val matcher = pattern.matcher(settingsField)
            if (!matcher.matches()) {
                // malformed
                return false
            }
            step = matcher.group(1).toInt()
            digits = TokenType.getFromString(matcher.group(2)).tokenDigits
        }

        type = OtpType.TOTP
        return true
    }

    private fun parseHOTPFromField(): Boolean {
        val secretField = getField(HMACOTP_SECRET_KEY)
        val secretHexField = getField(HMACOTP_SECRET_HEX_KEY)
        val secretBase32Field = getField(HMACOTP_SECRET_BASE32_KEY)
        val secretBase64Field = getField(HMACOTP_SECRET_BASE64_KEY)
        when {
            secretField != null -> secret = secretField.toByteArray(Charset.forName("UTF-8"))
            secretHexField != null -> try {
                secret = Hex.decodeHex(secretHexField)
            } catch (e: DecoderException) {
                e.printStackTrace()
                return false
            }
            secretBase32Field != null -> setBase32Secret(secretBase32Field)
            secretBase64Field != null -> setBase64Secret(secretBase64Field)
            else -> return false
        }

        val secretCounterField = getField(HMACOTP_SECRET_COUNTER_KEY)
        if (secretCounterField != null) {
            counter = secretCounterField.toInt()
        }

        type = OtpType.HOTP
        return true
    }

    private fun getField(id: String): String? {
        val field = entry.customFields[id]
        return field?.toString()
    }

    companion object {

        private val TAG = OtpEntryFields::class.java.name

        // Field from KeePassXC
        private const val OTP_FIELD = "otp"

        // URL parameters (https://github.com/google/google-authenticator/wiki/Key-Uri-Format)
        private const val OTP_SCHEME = "otpauth"
        private const val TOTP_AUTHORITY = "totp" // time-based
        private const val HOTP_AUTHORITY = "hotp" // counter-based
        private const val ALGORITHM_URL_PARAM = "algorithm"
        private const val ISSUER_URL_PARAM = "issuer"
        private const val SECRET_URL_PARAM = "secret"
        private const val DIGITS_URL_PARAM = "digits"
        private const val PERIOD_URL_PARAM = "period"
        private const val ENCODER_URL_PARAM = "encoder"
        private const val COUNTER_URL_PARAM = "counter"

        // Key-values (maybe from plugin or old KeePassXC)
        private const val SEED_KEY = "key"
        private const val DIGITS_KEY = "size"
        private const val STEP_KEY = "step"

        // HmacOtp KeePass2 values (https://keepass.info/help/base/placeholders.html#hmacotp)
        private const val HMACOTP_SECRET_KEY = "HmacOtp-Secret"
        private const val HMACOTP_SECRET_HEX_KEY = "HmacOtp-Secret-Hex"
        private const val HMACOTP_SECRET_BASE32_KEY = "HmacOtp-Secret-Base32"
        private const val HMACOTP_SECRET_BASE64_KEY = "HmacOtp-Secret-Base64"
        private const val HMACOTP_SECRET_COUNTER_KEY = "HmacOtp-Counter"

        // Custom fields (maybe from plugin)
        private const val TOTP_SEED_FIELD = "TOTP Seed"
        private const val TOTP_SETTING_FIELD = "TOTP Settings"

        // Logical breakdown of key=value regex. the final string is as follows:
        // [^&=\s]+=[^&=\s]+(&[^&=\s]+=[^&=\s]+)*
        private const val validKeyValue = "[^&=\\s]+"
        private const val validKeyValuePair = "$validKeyValue=$validKeyValue"
        private const val validKeyValueRegex = "$validKeyValuePair&($validKeyValuePair)*"

        private fun validateAndGetNameInPath(path: String?): String? {
            if (path == null || !path.startsWith("/")) {
                return null
            }
            // path is "/name", so remove leading "/", and trailing white spaces
            val name = path.substring(1).trim { it <= ' ' }
            return if (name.isEmpty()) {
                null // only white spaces.
            } else name
        }

        private fun breakDownKeyValuePairs(pairs: String): HashMap<String, String> {
            val elements = pairs.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val output = HashMap<String, String>()
            for (element in elements) {
                val pair = element.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                output[pair[0]] = pair[1]
            }
            return output
        }
    }
}
