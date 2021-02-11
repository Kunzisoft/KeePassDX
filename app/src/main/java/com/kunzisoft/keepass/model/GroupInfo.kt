package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard

class GroupInfo() : Parcelable {

    var name: String = ""
    var icon: IconImage = IconImageStandard()
    var notes: String? = null

    constructor(parcel: Parcel) : this() {
        name = parcel.readString() ?: name
        icon = parcel.readParcelable(IconImage::class.java.classLoader) ?: icon
        notes = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(icon, flags)
        parcel.writeString(notes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GroupInfo> {
        override fun createFromParcel(parcel: Parcel): GroupInfo {
            return GroupInfo(parcel)
        }

        override fun newArray(size: Int): Array<GroupInfo?> {
            return arrayOfNulls(size)
        }
    }
}