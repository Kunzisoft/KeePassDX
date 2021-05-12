package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.Parcelable

class TemplateSection: Parcelable {

    var attributes: List<TemplateAttribute> = ArrayList()
        private set

    constructor(attributes: List<TemplateAttribute>) {
        this.attributes = attributes
    }

    constructor(parcel: Parcel) {
        parcel.readList(attributes, TemplateAttribute::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(attributes)
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