package com.kunzisoft.keepass.otp

import com.kunzisoft.keepass.model.OtpModel
import org.apache.commons.codec.DecoderException
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
        set(value) {
            otpModel.counter = if (value < 0 && value > Long.MAX_VALUE) {
                TokenCalculator.HOTP_INITIAL_COUNTER
                throw NumberFormatException()
            } else value
        }

    var period
        get() = otpModel.period
        set(value) {
            otpModel.period = if (value <= 0 || value > 60) {
                TokenCalculator.TOTP_DEFAULT_PERIOD
                throw NumberFormatException()
            } else value
        }

    var digits
        get() = otpModel.digits
        set(value) {
            otpModel.digits = if (value <= 0|| value > 10) {
                TokenCalculator.OTP_DEFAULT_DIGITS
                throw NumberFormatException()
            } else value
        }

    var algorithm
        get() = otpModel.algorithm
        set(value) {
            otpModel.algorithm = value
        }

    fun setUTF8Secret(secret: String) {
        if (secret.isNotEmpty())
            otpModel.secret = secret.toByteArray(Charset.forName("UTF-8"))
        else
            throw DecoderException()
    }

    fun setHexSecret(secret: String) {
        if (secret.isNotEmpty())
            otpModel.secret = Hex.decodeHex(secret)
        else
            throw DecoderException()
    }

    fun getBase32Secret(): String {
        return otpModel.secret?.let {
            Base32().encodeAsString(it)
        } ?: ""
    }

    fun setBase32Secret(secret: String) {
        if (secret.isNotEmpty() && checkBase32Secret(secret))
            otpModel.secret = Base32().decode(secret.toByteArray())
        else
            throw DecoderException()
    }

    fun setBase64Secret(secret: String) {
        if (secret.isNotEmpty() && checkBase64Secret(secret))
            otpModel.secret = Base64().decode(secret.toByteArray())
        else
            throw DecoderException()
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