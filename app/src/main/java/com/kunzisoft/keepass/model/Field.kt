package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

class Field : Parcelable {

    var name: String? = null
    var value: String? = null

    constructor(name: String, value: String) {
        this.name = name
        this.value = value
    }

    constructor(parcel: Parcel) {
        this.name = parcel.readString()
        this.value = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(value)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<Field> = object : Parcelable.Creator<Field> {
            override fun createFromParcel(parcel: Parcel): Field {
                return Field(parcel)
            }

            override fun newArray(size: Int): Array<Field?> {
                return arrayOfNulls(size)
            }
        }
    }
}
