package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconImageStandard.Companion.FOLDER_ID

class GroupInfo : NodeInfo {

    var notes: String? = null

    init {
        icon = IconImageStandard(FOLDER_ID)
    }

    constructor(): super()

    constructor(parcel: Parcel): super(parcel) {
        notes = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeString(notes)
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