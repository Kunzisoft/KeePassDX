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
import com.kunzisoft.keepass.utils.CharArrayUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.CharArrayUtil.toUtf8ByteArray
import com.kunzisoft.keepass.utils.CodecUtil
import android.util.Base64
import com.kunzisoft.keepass.utils.clear
import java.nio.CharBuffer
import java.util.Locale
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
    fun setUTF8Secret(secret: CharArray) {
        if (secret.isNotEmpty())
            otpModel.secret = secret.toUtf8ByteArray()
        else
            throw IllegalArgumentException()
    }

    @Throws(IllegalArgumentException::class)
    fun setHexSecret(secret: CharArray) {
        if (secret.isNotEmpty())
            otpModel.secret = CodecUtil.decodeHex(secret)
        else
            throw IllegalArgumentException()
    }

    fun getBase32Secret(): CharArray {
        return otpModel.secret?.let {
            CodecUtil.encodeBase32(it)
        } ?: charArrayOf()
    }

    @Throws(IllegalArgumentException::class)
    fun setBase32Secret(secret: CharArray) {
        if (isValidBase32(secret)) {
            val secretChars = replaceBase32Chars(secret)
            otpModel.secret = CodecUtil.decodeBase32(secretChars)
        } else
            throw IllegalArgumentException()
    }

    @Throws(IllegalArgumentException::class)
    fun setBase64Secret(secret: CharArray) {
        if (isValidBase64(secret)) {
            val secretBytes = secret.toUtf8ByteArray()
            otpModel.secret = Base64.decode(secretBytes, Base64.DEFAULT)
            secretBytes.clear()
        } else
            throw IllegalArgumentException()
    }

    val token: CharArray
        get() {
            val secretBytes = secret ?: return charArrayOf()
            return when (type) {
                OtpType.HOTP -> TokenCalculator.getHotpToken(secretBytes, counter, digits, algorithm)
                OtpType.TOTP -> when (tokenType) {
                    OtpTokenType.STEAM -> TokenCalculator.getTotpSteamToken(secretBytes, period, digits, algorithm)
                    else -> TokenCalculator.getTotpRfc6238Token(secretBytes, period, digits, algorithm)
                }
            }
        }

    /**
     * Token with space each 3 digits
     */
    val tokenFormatted: CharArray
        get() {
            val t = token
            if (t.isEmpty()) return charArrayOf()
            val spaceCount = (t.size - 1) / 3
            val result = CharArray(t.size + spaceCount)
            var j = 0
            for (i in t.indices) {
                if (i > 0 && i % 3 == 0) {
                    result[j++] = ' '
                }
                result[j++] = t[i]
            }
            return result
        }

    val secondsRemaining: Int
        get() = otpModel.period - (System.currentTimeMillis() / 1000 % otpModel.period).toInt()

    fun shouldRefreshToken(): Boolean {
        return secondsRemaining == otpModel.period
    }

    fun clear() {
        otpModel.clear()
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

        const val MIN_OTP_SECRET = 8

        fun isValidCounter(counter: Long): Boolean {
            return counter in MIN_HOTP_COUNTER..MAX_HOTP_COUNTER
        }

        fun isValidPeriod(period: Int): Boolean {
            return period in MIN_TOTP_PERIOD..MAX_TOTP_PERIOD
        }

        fun isValidDigits(digits: Int): Boolean {
            return digits in MIN_OTP_DIGITS..MAX_OTP_DIGITS
        }

        fun isValidBase32(secret: CharArray): Boolean {
            val secretChars = replaceBase32Chars(secret)
            val charBuffer = CharBuffer.wrap(secretChars)
            return secretChars.isNotEmpty()
                    && (Pattern.matches("^(?:[A-Z2-7]{8})*(?:[A-Z2-7]{2}=*|[A-Z2-7]{4}=*|[A-Z2-7]{5}=*|[A-Z2-7]{7}=*)?$", charBuffer))
        }

        fun isValidBase64(secret: CharArray): Boolean {
            // TODO replace base 64 chars
            val charBuffer = CharBuffer.wrap(secret)
            return secret.isNotEmpty()
                    && (Pattern.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}=*|[A-Za-z0-9+/]{3}=*)?$", charBuffer))
        }

        private fun replaceBase32Chars(parameter: CharArray): CharArray {
            // Add padding '=' at end if not Base32 length
            var parameterNewSize = parameter
                .map { it.uppercaseChar() }
                .toCharArray()
                .removeSpaceChars()
            while (parameterNewSize.size % 8 != 0) {
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