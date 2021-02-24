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
    var expiryTime: DateInstant = DateInstant.IN_ONE_MONTH

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

    companion object CREATOR : Parcelable.Creator<NodeInfo> {
        override fun createFromParcel(parcel: Parcel): NodeInfo {
            return NodeInfo(parcel)
        }

        override fun newArray(size: Int): Array<NodeInfo?> {
            return arrayOfNulls(size)
        }
    }
}