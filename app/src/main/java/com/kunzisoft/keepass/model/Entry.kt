package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList

class Entry : Parcelable {

    var title: String = ""
    var username: String = ""
    var password: String = ""
    var url: String = ""
    var customFields: MutableList<Field> = ArrayList()

    constructor()

    private constructor(parcel: Parcel) {
        title = parcel.readString()
        username = parcel.readString()
        password = parcel.readString()
        url = parcel.readString()
        parcel.readList(customFields, Field::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(title)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeString(url)
        parcel.writeArray(customFields.toTypedArray())
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<Entry> = object : Parcelable.Creator<Entry> {
            override fun createFromParcel(parcel: Parcel): Entry {
                return Entry(parcel)
            }

            override fun newArray(size: Int): Array<Entry?> {
                return arrayOfNulls(size)
            }
        }
    }
}
