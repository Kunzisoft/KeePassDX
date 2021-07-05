package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.Parcelable

class TemplateSection: Parcelable {

    var name: String = ""
    var attributes: List<TemplateAttribute> = ArrayList()
        private set

    constructor(attributes: List<TemplateAttribute>, name: String = "") {
        this.name = name
        this.attributes = attributes
    }

    constructor(parcel: Parcel) {
        this.name = parcel.readString() ?: name
        parcel.readList(this.attributes, TemplateAttribute::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.name)
        parcel.writeList(this.attributes)
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