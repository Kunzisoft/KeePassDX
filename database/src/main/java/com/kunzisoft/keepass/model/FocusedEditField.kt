package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.utils.readParcelableCompat

class FocusedEditField : Parcelable {

    var field: Field? = null
    var cursorSelectionStart: Int = -1
    var cursorSelectionEnd: Int = -1

    constructor()

    constructor(parcel: Parcel) {
        this.field = parcel.readParcelableCompat()
        this.cursorSelectionStart = parcel.readInt()
        this.cursorSelectionEnd = parcel.readInt()
    }

    fun destroy() {
        this.field = null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(field, flags)
        parcel.writeInt(cursorSelectionStart)
        parcel.writeInt(cursorSelectionEnd)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FocusedEditField) return false

        if (field != other.field) return false

        return true
    }

    override fun hashCode(): Int {
        return field?.hashCode() ?: 0
    }

    companion object CREATOR : Parcelable.Creator<FocusedEditField> {
        override fun createFromParcel(parcel: Parcel): FocusedEditField {
            return FocusedEditField(parcel)
        }

        override fun newArray(size: Int): Array<FocusedEditField?> {
            return arrayOfNulls(size)
        }
    }
}