package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

data class RegisterInfo(val searchInfo: SearchInfo,
                        val username: String?,
                        val password: String?): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readParcelable(SearchInfo::class.java.classLoader) ?: SearchInfo(),
            parcel.readString() ?: "",
            parcel.readString() ?: "") {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(searchInfo, flags)
        parcel.writeString(username)
        parcel.writeString(password)
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