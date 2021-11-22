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
 * This code is based on KeePassXC code
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/totp/totp.cpp
 * https://github.com/keepassxreboot/keepassxc/blob/master/src/core/Entry.cpp
 */
package com.kunzisoft.keepass.otp

import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.otp.TokenCalculator.*
import com.kunzisoft.keepass.utils.StringUtil.removeLineChars
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import java.util.*
import java.util.regex.Pattern

object OtpEntryFields {

    private val TAG = OtpEntryFields::class.java.name

    // Field from KeePassXC
    const val OTP_FIELD = "otp"

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

    // OTPauth URI
    private const val REGEX_OTP_AUTH = "^(?:otpauth://([ht]otp)/)(?:(?:([^:?#]*): *)?([^:?#]*))(?:\\?([^#]+))$"

    // Key-values (maybe from plugin or old KeePassXC)
    private const val SEED_KEY = "key"
    private const val DIGITS_KEY = "size"
    private const val STEP_KEY = "step"

    // HmacOtp KeePass2 values (https://keepass.info/help/base/placeholders.html#otp)
    private const val HMACOTP_SECRET_FIELD = "HmacOtp-Secret"
    private const val HMACOTP_SECRET_HEX_FIELD = "HmacOtp-Secret-Hex"
    private const val HMACOTP_SECRET_BASE32_FIELD = "HmacOtp-Secret-Base32"
    private const val HMACOTP_SECRET_BASE64_FIELD = "HmacOtp-Secret-Base64"
    private const val HMACOTP_SECRET_COUNTER_FIELD = "HmacOtp-Counter"

    // TimeOtp KeePass2 values
    private const val TIMEOTP_SECRET_FIELD = "TimeOtp-Secret"
    private const val TIMEOTP_SECRET_HEX_FIELD = "TimeOtp-Secret-Hex"
    private const val TIMEOTP_SECRET_BASE32_FIELD = "TimeOtp-Secret-Base32"
    private const val TIMEOTP_SECRET_BASE64_FIELD = "TimeOtp-Secret-Base64"
    private const val TIMEOTP_LENGTH_FIELD = "TimeOtp-Length"
    private const val TIMEOTP_PERIOD_FIELD = "TimeOtp-Period"
    private const val TIMEOTP_ALGORITHM_FIELD = "TimeOtp-Algorithm"
    private const val TIMEOTP_ALGORITHM_SHA1_VALUE = "HMAC-SHA-1"
    private const val TIMEOTP_ALGORITHM_SHA256_VALUE = "HMAC-SHA-256"
    private const val TIMEOTP_ALGORITHM_SHA512_VALUE = "HMAC-SHA-512"

    // Custom fields (maybe from plugin)
    private const val TOTP_SEED_FIELD = "TOTP Seed"
    private const val TOTP_SETTING_FIELD = "TOTP Settings"

    // Token field, use dynamically to generate OTP token
    const val OTP_TOKEN_FIELD = "OTP Token"

    // Logical breakdown of key=value regex. the final string is as follows:
    // [^&=\s]+=[^&=\s]+(&[^&=\s]+=[^&=\s]+)*
    private const val validKeyValue = "[^&=\\s]+"
    private const val validKeyValuePair = "$validKeyValue=$validKeyValue"
    private const val validKeyValueRegex = "$validKeyValuePair(&$validKeyValuePair)*"

    /**
     * Parse fields of an entry to retrieve an OtpElement
     */
    fun parseFields(getField: (id: String) -> String?): OtpElement? {
        val otpElement = OtpElement()
        // OTP (HOTP/TOTP) from URL and field from KeePassXC
        if (parseOTPUri(getField, otpElement))
            return otpElement
        // TOTP from KeePass 2.47
        if (parseTOTPFromOfficialField(getField, otpElement))
            return otpElement
        // TOTP from key values (maybe plugin or old KeePassXC)
        if (parseTOTPKeyValues(getField, otpElement))
            return otpElement
        // TOTP from custom field
        if (parseTOTPFromPluginField(getField, otpElement))
            return otpElement
        // HOTP fields from KeePass 2
        if (parseHOTPFromOfficialField(getField, otpElement))
            return otpElement
        return null
    }

    /**
     * Tell if [otpUri] is a valid Otp URI
     */
    fun isOTPUri(otpUri: String): Boolean {
        if (Pattern.matches(REGEX_OTP_AUTH, otpUri))
            return true
        return false
    }

    /**
     * Get OtpElement from [otpUri]
     */
    fun parseOTPUri(otpUri: String): OtpElement? {
        val otpElement = OtpElement()
        if (parseOTPUri({ key -> if (key == OTP_FIELD) otpUri else null }, otpElement))
            return otpElement
        return null
    }

    /**
     * Parses a secret value from a URI. The format will be:
     *
     * otpauth://totp/user@example.com?secret=FFF...
     *
     * otpauth://hotp/user@example.com?secret=FFF...&counter=123
     */
    private fun parseOTPUri(getField: (id: String) -> String?, otpElement: OtpElement): Boolean {
        val otpPlainText = getField(OTP_FIELD)
        if (otpPlainText != null && otpPlainText.isNotEmpty() && isOTPUri(otpPlainText)) {
            val uri = Uri.parse(otpPlainText.removeSpaceChars())

            if (uri.scheme == null || OTP_SCHEME != uri.scheme!!.lowercase(Locale.ENGLISH)) {
                Log.e(TAG, "Invalid or missing scheme in uri")
                return false
            }

            val authority = uri.authority
            if (TOTP_AUTHORITY == authority) {
                otpElement.type = OtpType.TOTP

            } else if (HOTP_AUTHORITY == authority) {
                otpElement.type = OtpType.HOTP

                val counterParameter = uri.getQueryParameter(COUNTER_URL_PARAM)
                if (counterParameter != null) {
                    try {
                        otpElement.counter = counterParameter.toLongOrNull() ?: HOTP_INITIAL_COUNTER
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
            if (nameParam != null && nameParam.isNotEmpty()) {
                val userIdArray = nameParam.split(":", "%3A")
                if (userIdArray.size > 1) {
                    otpElement.issuer = userIdArray[0].removeLineChars()
                    otpElement.name = userIdArray[1].removeLineChars()
                } else {
                    otpElement.name = nameParam.removeLineChars()
                }
            }

            val issuerParam = uri.getQueryParameter(ISSUER_URL_PARAM)
            if (issuerParam != null && issuerParam.isNotEmpty())
                otpElement.issuer = issuerParam.removeLineChars()

            val secretParam = uri.getQueryParameter(SECRET_URL_PARAM)
            if (secretParam != null && secretParam.isNotEmpty()) {
                try {
                    otpElement.setBase32Secret(secretParam)
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to retrieve OTP secret.", exception)
                }
            }

            val encoderParam = uri.getQueryParameter(ENCODER_URL_PARAM)
            if (encoderParam != null && encoderParam.isNotEmpty())
                otpElement.tokenType = OtpTokenType.getFromString(encoderParam)

            val digitsParam = uri.getQueryParameter(DIGITS_URL_PARAM)
            if (digitsParam != null && digitsParam.isNotEmpty())
                try {
                    otpElement.digits = digitsParam.toIntOrNull() ?: OTP_DEFAULT_DIGITS
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to retrieve OTP digits.", exception)
                    otpElement.digits = OTP_DEFAULT_DIGITS
                }

            val counterParam = uri.getQueryParameter(COUNTER_URL_PARAM)
            if (counterParam != null && counterParam.isNotEmpty())
                try {
                    otpElement.counter = counterParam.toLongOrNull() ?: HOTP_INITIAL_COUNTER
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to retrieve HOTP counter.", exception)
                    otpElement.counter = HOTP_INITIAL_COUNTER
                }

            val stepParam = uri.getQueryParameter(PERIOD_URL_PARAM)
            if (stepParam != null && stepParam.isNotEmpty())
                try {
                    otpElement.period = stepParam.toIntOrNull() ?: TOTP_DEFAULT_PERIOD
                } catch (exception: Exception) {
                    Log.e(TAG, "Unable to retrieve TOTP period.", exception)
                    otpElement.period = TOTP_DEFAULT_PERIOD
                }

            val algorithmParam = uri.getQueryParameter(ALGORITHM_URL_PARAM)
            if (algorithmParam != null && algorithmParam.isNotEmpty()) {
                otpElement.algorithm = HashAlgorithm.fromString(algorithmParam)
            }

            return true
        }
        return false
    }

    private fun buildOtpUri(otpElement: OtpElement, title: String?, username: String?): Uri {
        val counterOrPeriodLabel: String
        val counterOrPeriodValue: String
        val otpAuthority: String

        when (otpElement.type) {
            OtpType.TOTP -> {
                counterOrPeriodLabel = PERIOD_URL_PARAM
                counterOrPeriodValue = otpElement.period.toString()
                otpAuthority = TOTP_AUTHORITY
            }
            else -> {
                counterOrPeriodLabel = COUNTER_URL_PARAM
                counterOrPeriodValue = otpElement.counter.toString()
                otpAuthority = HOTP_AUTHORITY
            }
        }
        val issuer =
                if (title != null && title.isNotEmpty())
                    encodeParameter(title)
                else
                    encodeParameter(otpElement.issuer)
        val accountName =
                if (username != null && username.isNotEmpty())
                    encodeParameter(username)
                else
                    encodeParameter(otpElement.name)
        val secret = encodeParameter(otpElement.getBase32Secret())
        val uriString = StringBuilder("otpauth://$otpAuthority/$issuer%3A$accountName" +
                "?$SECRET_URL_PARAM=${secret}" +
                "&$counterOrPeriodLabel=$counterOrPeriodValue" +
                "&$DIGITS_URL_PARAM=${otpElement.digits}" +
                "&$ISSUER_URL_PARAM=$issuer")
        if (otpElement.tokenType == OtpTokenType.STEAM) {
            uriString.append("&$ENCODER_URL_PARAM=${otpElement.tokenType}")
        } else {
            uriString.append("&$ALGORITHM_URL_PARAM=${otpElement.algorithm}")
        }

        return Uri.parse(uriString.toString())
    }

    private fun encodeParameter(parameter: String): String {
        return Uri.encode(parameter.removeLineChars())
    }

    private fun parseTOTPFromOfficialField(getField: (id: String) -> String?, otpElement: OtpElement): Boolean {
        val secretField = getField(TIMEOTP_SECRET_FIELD)
        val secretHexField = getField(TIMEOTP_SECRET_HEX_FIELD)
        val secretBase32Field = getField(TIMEOTP_SECRET_BASE32_FIELD)
        val secretBase64Field = getField(TIMEOTP_SECRET_BASE64_FIELD)
        val lengthField = getField(TIMEOTP_LENGTH_FIELD)
        val periodField = getField(TIMEOTP_PERIOD_FIELD)
        val algorithmField = getField(TIMEOTP_ALGORITHM_FIELD)
        try {
            when {
                secretField != null -> otpElement.setUTF8Secret(secretField)
                secretHexField != null -> otpElement.setHexSecret(secretHexField)
                secretBase32Field != null -> otpElement.setBase32Secret(secretBase32Field)
                secretBase64Field != null -> otpElement.setBase64Secret(secretBase64Field)
                else -> return false
            }
            otpElement.type = OtpType.TOTP
            if (lengthField != null) {
                otpElement.digits = lengthField.toIntOrNull() ?: OTP_DEFAULT_DIGITS
            }
            if (lengthField != null) {
                otpElement.digits = lengthField.toIntOrNull() ?: OTP_DEFAULT_DIGITS
            }
            if (periodField != null) {
                otpElement.period = periodField.toIntOrNull() ?: TOTP_DEFAULT_PERIOD
            }
            if (algorithmField != null) {
                otpElement.algorithm =
                        when (algorithmField.uppercase(Locale.ENGLISH)) {
                            TIMEOTP_ALGORITHM_SHA1_VALUE -> HashAlgorithm.SHA1
                            TIMEOTP_ALGORITHM_SHA256_VALUE -> HashAlgorithm.SHA256
                            TIMEOTP_ALGORITHM_SHA512_VALUE -> HashAlgorithm.SHA512
                            else -> HashAlgorithm.SHA1
                        }
            }
        } catch (exception: Exception) {
            return false
        }
        return true
    }

    private fun parseTOTPKeyValues(getField: (id: String) -> String?, otpElement: OtpElement): Boolean {
        val plainText = getField(OTP_FIELD)
        if (plainText != null && plainText.isNotEmpty()) {
            if (Pattern.matches(validKeyValueRegex, plainText)) {
                return try {
                    // KeeOtp string format
                    val query = breakDownKeyValuePairs(plainText)
                    otpElement.type = OtpType.TOTP
                    otpElement.setBase32Secret(query[SEED_KEY] ?: "")
                    otpElement.digits = query[DIGITS_KEY]?.toIntOrNull() ?: OTP_DEFAULT_DIGITS
                    otpElement.period = query[STEP_KEY]?.toIntOrNull() ?: TOTP_DEFAULT_PERIOD
                    true
                } catch (exception: Exception) {
                    false
                }
            } else {
                // Malformed
                return false
            }
        }
        return false
    }

    private fun parseTOTPFromPluginField(getField: (id: String) -> String?, otpElement: OtpElement): Boolean {
        val seedField = getField(TOTP_SEED_FIELD) ?: return false
        try {
            otpElement.setBase32Secret(seedField)

            val settingsField = getField(TOTP_SETTING_FIELD)
            if (settingsField != null) {
                // Regex match, sync with shortNameToEncoder
                val pattern = Pattern.compile("(\\d+);((?:\\d+)|S)")
                val matcher = pattern.matcher(settingsField)
                if (!matcher.matches()) {
                    // malformed
                    return false
                }
                otpElement.type = OtpType.TOTP
                otpElement.period = matcher.group(1)?.toIntOrNull() ?: TOTP_DEFAULT_PERIOD
                matcher.group(2)?.let { secondMatcher ->
                    try {
                        otpElement.digits = secondMatcher.toInt()
                    } catch (e: NumberFormatException) {
                        otpElement.digits = OTP_DEFAULT_DIGITS
                        otpElement.tokenType = OtpTokenType.getFromString(secondMatcher)
                    }
                }

            }
        } catch (exception: Exception) {
            return false
        }
        return true
    }

    private fun parseHOTPFromOfficialField(getField: (id: String) -> String?, otpElement: OtpElement): Boolean {
        val secretField = getField(HMACOTP_SECRET_FIELD)
        val secretHexField = getField(HMACOTP_SECRET_HEX_FIELD)
        val secretBase32Field = getField(HMACOTP_SECRET_BASE32_FIELD)
        val secretBase64Field = getField(HMACOTP_SECRET_BASE64_FIELD)
        val secretCounterField = getField(HMACOTP_SECRET_COUNTER_FIELD)
        try {
            when {
                secretField != null -> otpElement.setUTF8Secret(secretField)
                secretHexField != null -> otpElement.setHexSecret(secretHexField)
                secretBase32Field != null -> otpElement.setBase32Secret(secretBase32Field)
                secretBase64Field != null -> otpElement.setBase64Secret(secretBase64Field)
                else -> return false
            }
            otpElement.type = OtpType.HOTP
            if (secretCounterField != null) {
                otpElement.counter = secretCounterField.toLongOrNull() ?: HOTP_INITIAL_COUNTER
            }
        } catch (exception: Exception) {
            return false
        }
        return true
    }

    private fun validateAndGetNameInPath(path: String?): String? {
        if (path == null || !path.startsWith("/")) {
            return null
        }
        // path is "/name", so remove leading "/", and trailing white spaces
        val name = path.substring(1).trim { it <= ' ' }
        return if (name.isEmpty()) {
            null
        } else name
    }

    private fun breakDownKeyValuePairs(pairs: String): HashMap<String, String> {
        val elements = pairs.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val output = HashMap<String, String>()
        for (element in elements) {
            val pair = element.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            output[pair[0].lowercase(Locale.ENGLISH)] = pair[1]
        }
        return output
    }

    /**
     * Build Otp field from an OtpElement
     */
    fun buildOtpField(otpElement: OtpElement, title: String? = null, username: String? = null): Field {
        return Field(OTP_FIELD, ProtectedString(true,
                buildOtpUri(otpElement, title, username).toString()))
    }

    /**
     * Build new generated fields in a new list from [fieldsToParse] in parameter,
     * Remove parameters fields use to generate auto fields
     */
    fun generateAutoFields(fieldsToParse: List<Field>): MutableList<Field> {
        val newCustomFields: MutableList<Field> = ArrayList(fieldsToParse)
        // Remove parameter fields
        val otpField = Field(OTP_FIELD)
        val totpSeedField = Field(TOTP_SEED_FIELD)
        val totpSettingField = Field(TOTP_SETTING_FIELD)
        val hmacOtpSecretField = Field(HMACOTP_SECRET_FIELD)
        val hmacOtpSecretHexField = Field(HMACOTP_SECRET_HEX_FIELD)
        val hmacOtpSecretBase32Field = Field(HMACOTP_SECRET_BASE32_FIELD)
        val hmacOtpSecretBase64Field = Field(HMACOTP_SECRET_BASE64_FIELD)
        val hmacOtpSecretCounterField = Field(HMACOTP_SECRET_COUNTER_FIELD)
        val timeOtpSecretField = Field(TIMEOTP_SECRET_FIELD)
        val timeOtpSecretHexField = Field(TIMEOTP_SECRET_HEX_FIELD)
        val timeOtpSecretBase32Field = Field(TIMEOTP_SECRET_BASE32_FIELD)
        val timeOtpSecretBase64Field = Field(TIMEOTP_SECRET_BASE64_FIELD)
        val timeOtpLengthField = Field(TIMEOTP_LENGTH_FIELD)
        val timeOtpPeriodField = Field(TIMEOTP_PERIOD_FIELD)
        val timeOtpAlgorithmField = Field(TIMEOTP_ALGORITHM_FIELD)
        newCustomFields.remove(otpField)
        newCustomFields.remove(totpSeedField)
        newCustomFields.remove(totpSettingField)
        newCustomFields.remove(hmacOtpSecretField)
        newCustomFields.remove(hmacOtpSecretHexField)
        newCustomFields.remove(hmacOtpSecretBase32Field)
        newCustomFields.remove(hmacOtpSecretBase64Field)
        newCustomFields.remove(hmacOtpSecretCounterField)
        newCustomFields.remove(timeOtpSecretField)
        newCustomFields.remove(timeOtpSecretHexField)
        newCustomFields.remove(timeOtpSecretBase32Field)
        newCustomFields.remove(timeOtpSecretBase64Field)
        newCustomFields.remove(timeOtpLengthField)
        newCustomFields.remove(timeOtpPeriodField)
        newCustomFields.remove(timeOtpAlgorithmField)
        // Empty auto generated OTP Token field
        if (fieldsToParse.contains(otpField)
                || fieldsToParse.contains(totpSeedField)
                || fieldsToParse.contains(hmacOtpSecretField)
                || fieldsToParse.contains(hmacOtpSecretHexField)
                || fieldsToParse.contains(hmacOtpSecretBase32Field)
                || fieldsToParse.contains(hmacOtpSecretBase64Field)
                || fieldsToParse.contains(timeOtpSecretField)
                || fieldsToParse.contains(timeOtpSecretHexField)
                || fieldsToParse.contains(timeOtpSecretBase32Field)
                || fieldsToParse.contains(timeOtpSecretBase64Field)
        )
            newCustomFields.add(Field(OTP_TOKEN_FIELD))
        return newCustomFields
    }
}
