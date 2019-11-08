package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpTokenType
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.otp.TokenCalculator
import com.kunzisoft.keepass.otp.TokenCalculator.DEFAULT_ALGORITHM

class OtpModel() : Parcelable {

    var type: OtpType = OtpType.HOTP // ie : HOTP or TOTP
    var tokenType: OtpTokenType = OtpTokenType.RFC4226
    var name: String = "" // ie : user@email.com
    var issuer: String = "" // ie : Gitlab
    var secret: ByteArray = ByteArray(0) // Seed
    var counter: Int = TokenCalculator.HOTP_INITIAL_COUNTER // ie : 5 - only for HOTP
    var period: Int = TokenCalculator.TOTP_DEFAULT_PERIOD // ie : 30 seconds - only for TOTP
    var digits: Int = TokenCalculator.OTP_DEFAULT_DIGITS
    var algorithm: TokenCalculator.HashAlgorithm = DEFAULT_ALGORITHM

    constructor(parcel: Parcel) : this() {
        val typeRead = parcel.readInt()
        type = OtpType.values()[typeRead]
        tokenType = OtpTokenType.values()[parcel.readInt()]
        name = parcel.readString() ?: name
        issuer = parcel.readString() ?: issuer
        secret = parcel.createByteArray() ?: secret
        counter = parcel.readInt()
        period = parcel.readInt()
        digits = parcel.readInt()
        algorithm = TokenCalculator.HashAlgorithm.values()[parcel.readInt()]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OtpElement

        if (type != other.type) return false
        // Token type is important only if it's a TOTP
        if (type == OtpType.TOTP && tokenType != other.tokenType) return false
        if (!secret.contentEquals(other.secret)) return false
        // Counter only for HOTP
        if (type == OtpType.HOTP && counter != other.counter) return false
        // Step only for TOTP
        if (type == OtpType.TOTP && period != other.period) return false
        if (digits != other.digits) return false
        if (algorithm != other.algorithm) return false

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
        result = 31 * result + period
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
        parcel.writeInt(period)
        parcel.writeInt(digits)
        parcel.writeInt(algorithm.ordinal)
    }

    companion object CREATOR : Parcelable.Creator<OtpModel> {
        override fun createFromParcel(parcel: Parcel): OtpModel {
            return OtpModel(parcel)
        }

        override fun newArray(size: Int): Array<OtpModel?> {
            return arrayOfNulls(size)
        }
    }
}