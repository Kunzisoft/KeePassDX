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
package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpTokenType
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.otp.TokenCalculator
import com.kunzisoft.keepass.otp.TokenCalculator.OTP_DEFAULT_ALGORITHM

class OtpModel() : Parcelable {

    var type: OtpType = OtpType.TOTP // ie : HOTP or TOTP
    var tokenType: OtpTokenType = OtpTokenType.RFC6238
    var name: String = "OTP" // ie : user@email.com
    var issuer: String = "None" // ie : Gitlab
    var secret: ByteArray? = null // Seed
    var counter: Long = TokenCalculator.HOTP_INITIAL_COUNTER // ie : 5 - only for HOTP
    var period: Int = TokenCalculator.TOTP_DEFAULT_PERIOD // ie : 30 seconds - only for TOTP
    var digits: Int = TokenCalculator.OTP_DEFAULT_DIGITS
    var algorithm: TokenCalculator.HashAlgorithm = OTP_DEFAULT_ALGORITHM

    constructor(parcel: Parcel) : this() {
        val typeRead = parcel.readInt()
        type = OtpType.values()[typeRead]
        tokenType = OtpTokenType.values()[parcel.readInt()]
        name = parcel.readString() ?: name
        issuer = parcel.readString() ?: issuer
        secret = parcel.createByteArray() ?: secret
        counter = parcel.readLong()
        period = parcel.readInt()
        digits = parcel.readInt()
        algorithm = TokenCalculator.HashAlgorithm.values()[parcel.readInt()]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OtpModel

        if (type != other.type) return false
        // Token type is important only if it's a TOTP
        if (type == OtpType.TOTP && tokenType != other.tokenType) return false
        if (secret == null || other.secret == null) return false
        if (!secret!!.contentEquals(other.secret!!)) return false
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
        result = 31 * result + name.hashCode()
        result = 31 * result + issuer.hashCode()
        result = 31 * result + (secret?.contentHashCode() ?: 0)
        result = 31 * result + counter.hashCode()
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
        parcel.writeLong(counter)
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