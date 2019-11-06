package com.kunzisoft.keepass.otp

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.otp.TokenCalculator.DEFAULT_ALGORITHM

data class OtpElement(var type: OtpType = OtpType.UNDEFINED, // ie : HOTP or TOTP
                      var tokenType: TokenType = TokenType.Default,
                      var name: String = "", // ie : user@email.com
                      var issuer: String = "", // ie : Gitlab
                      var secret: ByteArray = ByteArray(0), // Seed
                      var counter: Int = TokenCalculator.HOTP_INITIAL_COUNTER, // ie : 5 - only for HOTP
                      var step: Int = TokenCalculator.TOTP_DEFAULT_PERIOD, // ie : 30 seconds - only for TOTP
                      var digits: Int = TokenType.Default.tokenDigits,
                      var algorithm: TokenCalculator.HashAlgorithm = DEFAULT_ALGORITHM
                      ) : Parcelable {

    constructor(parcel: Parcel) : this(
            OtpType.values()[parcel.readInt()],
            TokenType.values()[parcel.readInt()],
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.createByteArray() ?: ByteArray(0),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            TokenCalculator.HashAlgorithm.values()[parcel.readInt()])

    fun setSettings(seed: String, digits: Int, step: Int) {
        // TODO: Implement a way to set TOTP from device
    }

    val token: String
        get() {
            return when (type) {
                OtpType.HOTP -> TokenCalculator.HOTP(secret, counter.toLong(), digits, algorithm)
                OtpType.TOTP -> when (tokenType) {
                    TokenType.Steam -> TokenCalculator.TOTP_Steam(secret, this.step, digits, algorithm)
                    TokenType.Default -> TokenCalculator.TOTP_RFC6238(secret, this.step, digits, algorithm)
                }
                OtpType.UNDEFINED -> ""
            }
        }

    val secondsRemaining: Int
        get() = step - (System.currentTimeMillis() / 1000 % step).toInt()

    fun shouldRefreshToken(): Boolean {
        return secondsRemaining == this.step
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OtpElement

        if (type != other.type) return false
        // Other values only for defined element
        if (type != OtpType.UNDEFINED) {
            // Token type is important only if it's a TOTP
            if (type == OtpType.TOTP && tokenType != other.tokenType) return false
            if (!secret.contentEquals(other.secret)) return false
            // Counter only for HOTP
            if (type == OtpType.HOTP && counter != other.counter) return false
            // Step only for TOTP
            if (type == OtpType.TOTP && step != other.step) return false
            if (digits != other.digits) return false
            if (algorithm != other.algorithm) return false
        }

        return true
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + tokenType.hashCode()
        result = 31 * result + secret.contentHashCode()
        result = 31 * result + counter
        result = 31 * result + step
        result = 31 * result + digits
        result = 31 * result + algorithm.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type.ordinal)
        parcel.writeInt(tokenType.ordinal)
        parcel.writeString(name)
        parcel.writeString(issuer)
        parcel.writeByteArray(secret)
        parcel.writeInt(counter)
        parcel.writeInt(step)
        parcel.writeInt(digits)
        parcel.writeInt(algorithm.ordinal)
    }

    companion object CREATOR : Parcelable.Creator<OtpElement> {
        override fun createFromParcel(parcel: Parcel): OtpElement {
            return OtpElement(parcel)
        }

        override fun newArray(size: Int): Array<OtpElement?> {
            return arrayOfNulls(size)
        }
    }
}

enum class OtpType {
    UNDEFINED,
    HOTP,   // counter based
    TOTP    // time based
}

enum class TokenType (var tokenDigits: Int) {
    Default(TokenCalculator.TOTP_DEFAULT_DIGITS),
    Steam(TokenCalculator.STEAM_DEFAULT_DIGITS);

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