package com.kunzisoft.keepass.otp

import com.kunzisoft.keepass.model.OtpModel
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
            otpModel.counter = if (value < MIN_HOTP_COUNTER || value > MAX_HOTP_COUNTER) {
                TokenCalculator.HOTP_INITIAL_COUNTER
                throw IllegalArgumentException()
            } else value
        }

    var period
        get() = otpModel.period
        @Throws(NumberFormatException::class)
        set(value) {
            otpModel.period = if (value < MIN_TOTP_PERIOD || value > MAX_TOTP_PERIOD) {
                TokenCalculator.TOTP_DEFAULT_PERIOD
                throw NumberFormatException()
            } else value
        }

    var digits
        get() = otpModel.digits
        @Throws(NumberFormatException::class)
        set(value) {
            otpModel.digits = if (value < MIN_OTP_DIGITS|| value > MAX_OTP_DIGITS) {
                TokenCalculator.OTP_DEFAULT_DIGITS
                throw NumberFormatException()
            } else value
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
            otpModel.secret = Hex.decodeHex(secret)
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
        if (secret.isNotEmpty() && checkBase32Secret(secret))
            otpModel.secret = Base32().decode(secret.toByteArray())
        else
            throw IllegalArgumentException()
    }

    @Throws(IllegalArgumentException::class)
    fun setBase64Secret(secret: String) {
        if (secret.isNotEmpty() && checkBase64Secret(secret))
            otpModel.secret = Base64().decode(secret.toByteArray())
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

    val secondsRemaining: Int
        get() = otpModel.period - (System.currentTimeMillis() / 1000 % otpModel.period).toInt()

    fun shouldRefreshToken(): Boolean {
        return secondsRemaining == otpModel.period
    }

    companion object {
        const val MIN_HOTP_COUNTER = 1
        const val MAX_HOTP_COUNTER = Long.MAX_VALUE

        const val MIN_TOTP_PERIOD = 1
        const val MAX_TOTP_PERIOD = 60

        const val MIN_OTP_DIGITS = 4
        const val MAX_OTP_DIGITS = 18

        fun checkBase32Secret(secret: String): Boolean {
            return (Pattern.matches("^(?:[A-Z2-7]{8})*(?:[A-Z2-7]{2}={6}|[A-Z2-7]{4}={4}|[A-Z2-7]{5}={3}|[A-Z2-7]{7}=)?$", secret))
        }

        fun checkBase64Secret(secret: String): Boolean {
            return (Pattern.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$", secret))
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
            return when (tokenType.toLowerCase(Locale.ENGLISH)) {
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