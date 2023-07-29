/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.otp

import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern

data class OtpElement(var otpModel: OtpModel = OtpModel()) {

    var type
        get() = otpModel.type
        set(value) {
            otpModel.type = value
            if (type == OtpType.HOTP) {
                if (!OtpTokenType.getHotpTokenTypeValues().contains(tokenType))
                    tokenType = OtpTokenType.RFC4226
            }
            if (type == OtpType.TOTP) {
                if (!OtpTokenType.getTotpTokenTypeValues().contains(tokenType))
                    tokenType = OtpTokenType.RFC6238
            }
        }

    var tokenType
        get() = otpModel.tokenType
        set(value) {
            otpModel.tokenType = value
            when (tokenType) {
                OtpTokenType.RFC4226 -> {
                    otpModel.algorithm = TokenCalculator.OTP_DEFAULT_ALGORITHM
                    otpModel.digits = TokenCalculator.OTP_DEFAULT_DIGITS
                    otpModel.counter = TokenCalculator.HOTP_INITIAL_COUNTER
                }
                OtpTokenType.RFC6238 -> {
                    otpModel.algorithm = TokenCalculator.OTP_DEFAULT_ALGORITHM
                    otpModel.digits = TokenCalculator.OTP_DEFAULT_DIGITS
                    otpModel.period = TokenCalculator.TOTP_DEFAULT_PERIOD
                }
                OtpTokenType.STEAM -> {
                    otpModel.algorithm = TokenCalculator.OTP_DEFAULT_ALGORITHM
                    otpModel.digits = TokenCalculator.STEAM_DEFAULT_DIGITS
                    otpModel.period = TokenCalculator.TOTP_DEFAULT_PERIOD
                }
            }
        }

    var name
        get() = otpModel.name
        set(value) {
            otpModel.name = value
        }

    var issuer
        get() = otpModel.issuer
        set(value) {
            otpModel.issuer = value
        }

    var secret
        get() = otpModel.secret
        private set(value) {
            otpModel.secret = value
        }

    var counter
        get() = otpModel.counter
        @Throws(NumberFormatException::class)
        set(value) {
            otpModel.counter = if (isValidCounter(value)) {
                value
            } else {
                TokenCalculator.HOTP_INITIAL_COUNTER
                throw NumberFormatException()
            }
        }

    var period
        get() = otpModel.period
        @Throws(NumberFormatException::class)
        set(value) {
            otpModel.period = if (isValidPeriod(value)) {
                value
            } else {
                TokenCalculator.TOTP_DEFAULT_PERIOD
                throw NumberFormatException()
            }
        }

    var digits
        get() = otpModel.digits
        @Throws(NumberFormatException::class)
        set(value) {
            otpModel.digits = if (isValidDigits(value)) {
                value
            } else {
                TokenCalculator.OTP_DEFAULT_DIGITS
                throw NumberFormatException()
            }
        }

    var algorithm
        get() = otpModel.algorithm
        set(value) {
            otpModel.algorithm = value
        }

    @Throws(IllegalArgumentException::class)
    fun setUTF8Secret(secret: String) {
        if (secret.isNotEmpty())
            otpModel.secret = secret.toByteArray(Charset.forName("UTF-8"))
        else
            throw IllegalArgumentException()
    }

    @Throws(IllegalArgumentException::class)
    fun setHexSecret(secret: String) {
        if (secret.isNotEmpty())
            otpModel.secret = Hex.decodeHex(secret.toCharArray())
        else
            throw IllegalArgumentException()
    }

    fun getBase32Secret(): String {
        return otpModel.secret?.let {
            Base32().encodeAsString(it)
        } ?: ""
    }

    @Throws(IllegalArgumentException::class)
    fun setBase32Secret(secret: String) {
        if (isValidBase32(secret)) {
            otpModel.secret = Base32().decode(replaceBase32Chars(secret))
        } else
            throw IllegalArgumentException()
    }

    @Throws(IllegalArgumentException::class)
    fun setBase64Secret(secret: String) {
        if (isValidBase64(secret))
            otpModel.secret = Base64().decode(secret)
        else
            throw IllegalArgumentException()
    }

    val token: String
        get() {
            if (secret == null)
                return ""
            return when (type) {
                OtpType.HOTP -> TokenCalculator.HOTP(secret, counter, digits, algorithm)
                OtpType.TOTP -> when (tokenType) {
                    OtpTokenType.STEAM -> TokenCalculator.TOTP_Steam(secret, period, digits, algorithm)
                    else -> TokenCalculator.TOTP_RFC6238(secret, period, digits, algorithm)
                }
            }
        }

    /**
     * Token with space each 3 digits
     */
    val tokenString: String
        get() {
            return token.replace("...".toRegex(), "$0 ")
        }

    val secondsRemaining: Int
        get() = otpModel.period - (System.currentTimeMillis() / 1000 % otpModel.period).toInt()

    fun shouldRefreshToken(): Boolean {
        return secondsRemaining == otpModel.period
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OtpElement) return false

        if (otpModel != other.otpModel) return false

        return true
    }

    override fun hashCode(): Int {
        return otpModel.hashCode()
    }

    companion object {
        const val MIN_HOTP_COUNTER = 0
        const val MAX_HOTP_COUNTER = Long.MAX_VALUE

        const val MIN_TOTP_PERIOD = 1
        const val MAX_TOTP_PERIOD = 900

        const val MIN_OTP_DIGITS = 4
        const val MAX_OTP_DIGITS = 18

        fun isValidCounter(counter: Long): Boolean {
            return counter in MIN_HOTP_COUNTER..MAX_HOTP_COUNTER
        }

        fun isValidPeriod(period: Int): Boolean {
            return period in MIN_TOTP_PERIOD..MAX_TOTP_PERIOD
        }

        fun isValidDigits(digits: Int): Boolean {
            return digits in MIN_OTP_DIGITS..MAX_OTP_DIGITS
        }

        fun isValidBase32(secret: String): Boolean {
            val secretChars = replaceBase32Chars(secret)
            return secret.isNotEmpty()
                    && (Pattern.matches("^(?:[A-Z2-7]{8})*(?:[A-Z2-7]{2}=*|[A-Z2-7]{4}=*|[A-Z2-7]{5}=*|[A-Z2-7]{7}=*)?$", secretChars))
        }

        fun isValidBase64(secret: String): Boolean {
            // TODO replace base 64 chars
            return secret.isNotEmpty()
                    && (Pattern.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}=*|[A-Za-z0-9+/]{3}=*)?$", secret))
        }

        fun replaceBase32Chars(parameter: String): String {
            // Add padding '=' at end if not Base32 length
            var parameterNewSize = parameter.uppercase(Locale.ENGLISH).removeSpaceChars()
            while (parameterNewSize.length % 8 != 0) {
                parameterNewSize += '='
            }
            return parameterNewSize
        }
    }
}

enum class OtpType {
    HOTP,   // counter based
    TOTP;    // time based
}

enum class OtpTokenType {
    RFC4226,    // HOTP
    RFC6238,    // TOTP

    // Proprietary
    STEAM;    // TOTP Steam

    override fun toString(): String {
        return when (this) {
            STEAM -> "steam"
            else -> super.toString()
        }
    }

    companion object {
        fun getFromString(tokenType: String): OtpTokenType {
            return when (tokenType.lowercase(Locale.ENGLISH)) {
                "s", "steam" -> STEAM
                "hotp" -> RFC4226
                else -> RFC6238
            }
        }

        fun getTotpTokenTypeValues(getProprietaryElements: Boolean = true): Array<OtpTokenType> {
            return if (getProprietaryElements)
                arrayOf(RFC6238, STEAM)
            else
                arrayOf(RFC6238)
        }

        fun getHotpTokenTypeValues(): Array<OtpTokenType> {
            return  arrayOf(RFC4226)
        }
    }
}