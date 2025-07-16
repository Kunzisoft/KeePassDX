package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readParcelableCompat

data class RegisterInfo(
    val searchInfo: SearchInfo,
    val username: String?,
    val password: String? = null,
    val creditCard: CreditCard? = null,
    val passkey: Passkey? = null
): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat() ?: SearchInfo(),
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readParcelableCompat(),
            parcel.readParcelableCompat()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(searchInfo, flags)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeParcelable(creditCard, flags)
        parcel.writeParcelable(passkey, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RegisterInfo> {
        override fun createFromParcel(parcel: Parcel): RegisterInfo {
            return RegisterInfo(parcel)
        }

        override fun newArray(size: Int): Array<RegisterInfo?> {
            return arrayOfNulls(size)
        }
    }
}