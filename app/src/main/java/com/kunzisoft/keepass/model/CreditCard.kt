package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

data class CreditCard(val cardholder: String?,
                      val number: String?,
                      val expiration: String?,
                      val cvv: String?) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cardholder)
        parcel.writeString(number)
        parcel.writeString(expiration)
        parcel.writeString(cvv)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CreditCard> {
        override fun createFromParcel(parcel: Parcel): CreditCard {
            return CreditCard(parcel)
        }

        override fun newArray(size: Int): Array<CreditCard?> {
            return arrayOfNulls(size)
        }
    }
}
