/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.database.element.template

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readListCompat

class TemplateSection: Parcelable {

    var name: String = ""
    var attributes: MutableList<TemplateAttribute> = mutableListOf()
        private set

    constructor(attributes: MutableList<TemplateAttribute>, name: String = "") {
        this.name = name
        this.attributes = attributes
    }

    constructor(parcel: Parcel) {
        this.name = parcel.readString() ?: name
        parcel.readListCompat(this.attributes)
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