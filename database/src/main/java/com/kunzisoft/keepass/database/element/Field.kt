/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.readParcelableCompat

class Field : Parcelable {

    var name: String = ""
    var protectedValue: ProtectedString = ProtectedString()

    constructor(name: String, value: ProtectedString = ProtectedString()) {
        this.name = name
        this.protectedValue = value
    }

    constructor(fieldToCopy: Field) {
        this.name = fieldToCopy.name
        this.protectedValue = fieldToCopy.protectedValue
    }

    constructor(parcel: Parcel) {
        this.name = parcel.readString() ?: name
        this.protectedValue = parcel.readParcelableCompat() ?: protectedValue
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
        if (other !is Field) return false

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
