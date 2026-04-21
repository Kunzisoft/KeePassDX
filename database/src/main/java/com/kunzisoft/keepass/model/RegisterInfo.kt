package com.kunzisoft.keepass.model

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.utils.ObjectNameResource
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.readParcelableCompat

data class RegisterInfo(
    val searchInfo: SearchInfo,
    val username: String? = null,
    val password: CharArray? = null,
    val expiration: DateInstant? = null,
    val creditCard: CreditCard? = null,
    val passkey: Passkey? = null,
    val appOrigin: AppOrigin? = null
) : ObjectNameResource, Parcelable {

    constructor(parcel: Parcel) : this(
        searchInfo = parcel.readParcelableCompat() ?: SearchInfo(),
        username = parcel.readString(),
        password = parcel.createCharArray(),
        expiration = parcel.readParcelableCompat(),
        creditCard = parcel.readParcelableCompat(),
        passkey = parcel.readParcelableCompat(),
        appOrigin = parcel.readParcelableCompat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(searchInfo, flags)
        parcel.writeString(username)
        parcel.writeCharArray(password)
        parcel.writeParcelable(expiration, flags)
        parcel.writeParcelable(creditCard, flags)
        parcel.writeParcelable(passkey, flags)
        parcel.writeParcelable(appOrigin, flags)
    }

    fun clear() {
        password?.clear()
        creditCard?.clear()
        passkey?.clear()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun getName(resources: Resources): String {
        if (username != null)
            return "$username (${searchInfo.getName(resources)})"
        return passkey?.relyingParty
            ?: appOrigin?.toName()
            ?: searchInfo.getName(resources)
    }

    override fun toString(): String {
        if (username != null)
            return "$username ($searchInfo)"
        return passkey?.relyingParty
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegisterInfo

        if (searchInfo != other.searchInfo) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (expiration != other.expiration) return false
        if (creditCard != other.creditCard) return false
        if (passkey != other.passkey) return false
        if (appOrigin != other.appOrigin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = searchInfo.hashCode()
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + (password?.contentHashCode() ?: 0)
        result = 31 * result + (expiration?.hashCode() ?: 0)
        result = 31 * result + (creditCard?.hashCode() ?: 0)
        result = 31 * result + (passkey?.hashCode() ?: 0)
        result = 31 * result + (appOrigin?.hashCode() ?: 0)
        return result
    }
}