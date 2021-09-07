package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage

open class NodeInfo() : Parcelable {

    var title: String = ""
    var icon: IconImage = IconImage()
    var creationTime: DateInstant = DateInstant()
    var lastModificationTime: DateInstant = DateInstant()
    var expires: Boolean = false
    var expiryTime: DateInstant = DateInstant.IN_ONE_MONTH_DATE_TIME

    constructor(parcel: Parcel) : this() {
        title = parcel.readString() ?: title
        icon = parcel.readParcelable(IconImage::class.java.classLoader) ?: icon
        creationTime = parcel.readParcelable(DateInstant::class.java.classLoader) ?: creationTime
        lastModificationTime = parcel.readParcelable(DateInstant::class.java.classLoader) ?: lastModificationTime
        expires = parcel.readInt() != 0
        expiryTime = parcel.readParcelable(DateInstant::class.java.classLoader) ?: expiryTime
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(creationTime, flags)
        parcel.writeParcelable(lastModificationTime, flags)
        parcel.writeInt(if (expires) 1 else 0)
        parcel.writeParcelable(expiryTime, flags)
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

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + lastModificationTime.hashCode()
        result = 31 * result + expires.hashCode()
        result = 31 * result + expiryTime.hashCode()
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