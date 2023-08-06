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
package com.kunzisoft.keepass.database.element.security

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat

class ProtectedString : Parcelable {

    var isProtected: Boolean = false
        private set
    var stringValue: String = ""

    constructor(toCopy: ProtectedString) {
        this.isProtected = toCopy.isProtected
        this.stringValue = toCopy.stringValue
    }

    constructor(enableProtection: Boolean = false, string: String = "") {
        this.isProtected = enableProtection
        this.stringValue = string
    }

    constructor(parcel: Parcel) {
        isProtected = parcel.readBooleanCompat()
        stringValue = parcel.readString() ?: stringValue
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeBooleanCompat(isProtected)
        dest.writeString(stringValue)
    }

    fun length(): Int {
        return stringValue.length
    }

    override fun toString(): String {
        return stringValue
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<ProtectedString> = object : Parcelable.Creator<ProtectedString> {
            override fun createFromParcel(parcel: Parcel): ProtectedString {
                return ProtectedString(parcel)
            }

            override fun newArray(size: Int): Array<ProtectedString?> {
                return arrayOfNulls(size)
            }
        }
    }

}
