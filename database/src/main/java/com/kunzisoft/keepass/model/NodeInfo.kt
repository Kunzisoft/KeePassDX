package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat

open class NodeInfo() : Parcelable {

    var title: String = ""
    var icon: IconImage = IconImage()
    var creationTime: DateInstant = DateInstant()
    var lastModificationTime: DateInstant = DateInstant()
    var expires: Boolean = false
    var expiryTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME
    var customData: CustomData = CustomData()

    constructor(parcel: Parcel) : this() {
        title = parcel.readString() ?: title
        icon = parcel.readParcelableCompat() ?: icon
        creationTime = parcel.readParcelableCompat() ?: creationTime
        lastModificationTime = parcel.readParcelableCompat() ?: lastModificationTime
        expires = parcel.readBooleanCompat()
        expiryTime = parcel.readParcelableCompat() ?: expiryTime
        customData = parcel.readParcelableCompat() ?: customData
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(creationTime, flags)
        parcel.writeParcelable(lastModificationTime, flags)
        parcel.writeBooleanCompat(expires)
        parcel.writeParcelable(expiryTime, flags)
        parcel.writeParcelable(customData, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeInfo) return false

        if (title != other.title) return false
        if (icon != other.icon) return false
        if (creationTime != other.creationTime) return false
        if (lastModificationTime != other.lastModificationTime) return false
        if (expires != other.expires) return false
        if (expiryTime != other.expiryTime) return false
        if (customData != other.customData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + lastModificationTime.hashCode()
        result = 31 * result + expires.hashCode()
        result = 31 * result + expiryTime.hashCode()
        result = 31 * result + customData.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<NodeInfo> {
        override fun createFromParcel(parcel: Parcel): NodeInfo {
            return NodeInfo(parcel)
        }

        override fun newArray(size: Int): Array<NodeInfo?> {
            return arrayOfNulls(size)
        }
    }
}