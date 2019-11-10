package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.ProtectedString

class Field : Parcelable {

    var name: String = ""
    var protectedValue: ProtectedString = ProtectedString()

    constructor(name: String, value: ProtectedString = ProtectedString()) {
        this.name = name
        this.protectedValue = value
    }

    constructor(parcel: Parcel) {
        this.name = parcel.readString() ?: name
        this.protectedValue = parcel.readParcelable(ProtectedString::class.java.classLoader) ?: protectedValue
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeParcelable(protectedValue, flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Field

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
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
