package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readParcelableCompat

class CustomDataItem : Parcelable {

    val key: String
    var value: String
    var lastModificationTime: DateInstant? = null

    constructor(parcel: Parcel) {
        key = parcel.readString() ?: ""
        value = parcel.readString() ?: ""
        lastModificationTime = parcel.readParcelableCompat()
    }

    constructor(key: String, value: String, lastModificationTime: DateInstant? = null) {
        this.key = key
        this.value = value
        this.lastModificationTime = lastModificationTime
    }

    override fun toString(): String {
        return value
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(value)
        parcel.writeParcelable(lastModificationTime, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomDataItem> {
        override fun createFromParcel(parcel: Parcel): CustomDataItem {
            return CustomDataItem(parcel)
        }

        override fun newArray(size: Int): Array<CustomDataItem?> {
            return arrayOfNulls(size)
        }
    }
}