package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList

class EntryInfo : Parcelable {

    var id: String = ""
    var title: String = ""
    var username: String = ""
    var password: String = ""
    var url: String = ""
    var notes: String = ""
    var customFields: MutableList<Field> = ArrayList()

    constructor()

    private constructor(parcel: Parcel) {
        id = parcel.readString() ?: id
        title = parcel.readString() ?: title
        username = parcel.readString() ?: username
        password = parcel.readString() ?: password
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
        parcel.readList(customFields, Field::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeString(url)
        parcel.writeString(notes)
        parcel.writeArray(customFields.toTypedArray())
    }

    fun containsCustomFieldsProtected(): Boolean {
        return customFields.any { it.protectedValue.isProtected }
    }

    fun containsCustomFieldsNotProtected(): Boolean {
        return customFields.any { !it.protectedValue.isProtected }
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<EntryInfo> = object : Parcelable.Creator<EntryInfo> {
            override fun createFromParcel(parcel: Parcel): EntryInfo {
                return EntryInfo(parcel)
            }

            override fun newArray(size: Int): Array<EntryInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}
