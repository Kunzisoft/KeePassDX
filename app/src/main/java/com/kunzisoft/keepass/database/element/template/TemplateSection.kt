package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.Parcelable

class TemplateSection: Parcelable {

    var attributes: List<TemplateAttribute> = ArrayList()
        private set
    var dynamic: Boolean = false
        private set

    constructor() {
        this.dynamic = true
    }

    constructor(attributes: List<TemplateAttribute>, dynamic: Boolean = false) {
        this.attributes = attributes
        this.dynamic = dynamic
    }

    constructor(parcel: Parcel) {
        parcel.readList(attributes, TemplateAttribute::class.java.classLoader)
        parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(attributes)
        parcel.writeByte((if (dynamic) 1 else 0).toByte())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TemplateSection> {
        override fun createFromParcel(parcel: Parcel): TemplateSection {
            return TemplateSection(parcel)
        }

        override fun newArray(size: Int): Array<TemplateSection?> {
            return arrayOfNulls(size)
        }
    }
}