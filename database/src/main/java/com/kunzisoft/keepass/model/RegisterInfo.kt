package com.kunzisoft.keepass.model

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.ObjectNameResource
import com.kunzisoft.keepass.utils.readParcelableCompat

data class RegisterInfo(
    val searchInfo: SearchInfo,
    val username: String? = null,
    val password: String? = null,
    val creditCard: CreditCard? = null,
    val passkey: Passkey? = null,
    val appOrigin: AppOrigin? = null
) : ObjectNameResource, Parcelable {

    constructor(parcel: Parcel) : this(
        searchInfo = parcel.readParcelableCompat() ?: SearchInfo(),
        username = parcel.readString(),
        password = parcel.readString(),
        creditCard = parcel.readParcelableCompat(),
        passkey = parcel.readParcelableCompat(),
        appOrigin = parcel.readParcelableCompat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(searchInfo, flags)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeParcelable(creditCard, flags)
        parcel.writeParcelable(passkey, flags)
        parcel.writeParcelable(appOrigin, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun getName(resources: Resources): String {
        return username
            ?: passkey?.relyingParty
            ?: appOrigin?.toName()
            ?: searchInfo.getName(resources)
    }

    override fun toString(): String {
        return username
            ?: passkey?.relyingParty
            ?: appOrigin?.toName()
            ?: searchInfo.toString()
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