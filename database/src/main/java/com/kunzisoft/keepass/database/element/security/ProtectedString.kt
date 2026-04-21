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
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readCharArrayCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import com.kunzisoft.keepass.utils.writeCharArrayCompat

class ProtectedString : Parcelable {

    var isProtected: Boolean = false
        private set
    var charArrayValue: CharArray = charArrayOf()
        private set

    constructor(toCopy: ProtectedString) {
        this.isProtected = toCopy.isProtected
        this.charArrayValue = toCopy.charArrayValue.copyOf()
    }

    // TODO Remove
    constructor(enableProtection: Boolean = false, value: String) {
        this.isProtected = enableProtection
        this.charArrayValue = value.toCharArray()
    }

    constructor(enableProtection: Boolean = false, value: CharArray? = null) {
        this.isProtected = enableProtection
        this.charArrayValue = value?.copyOf() ?: charArrayOf()
    }

    constructor(parcel: Parcel) {
        isProtected = parcel.readBooleanCompat()
        charArrayValue = parcel.readCharArrayCompat() ?: charArrayOf()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeBooleanCompat(isProtected)
        dest.writeCharArrayCompat(charArrayValue)
    }

    fun length(): Int {
        return charArrayValue.size
    }

    override fun toString(): String {
        return String(charArrayValue)
    }

    // TODO Use to properly clear data
    fun clear() {
        charArrayValue.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProtectedString

        if (isProtected != other.isProtected) return false
        if (!charArrayValue.contentEquals(other.charArrayValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isProtected.hashCode()
        result = 31 * result + charArrayValue.contentHashCode()
        return result
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

        fun String.toBooleanCompat(): Boolean {
            return if (this.equals("1", ignoreCase = true))
                true
            else
                this.toBoolean()
        }

        fun Boolean.toFieldValue(): String {
            return if (this) "1" else "0"
        }
    }

}
