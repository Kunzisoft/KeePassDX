/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.entry

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat

class AutoType : Parcelable {

    var enabled = true
    var obfuscationOptions = OBF_OPT_NONE
    var defaultSequence = ""
    private var windowSeqPairs = ArrayList<AutoTypeItem>()

    constructor()

    constructor(autoType: AutoType) {
        this.enabled = autoType.enabled
        this.obfuscationOptions = autoType.obfuscationOptions
        this.defaultSequence = autoType.defaultSequence
        this.windowSeqPairs.clear()
        this.windowSeqPairs.addAll(autoType.windowSeqPairs)
    }

    constructor(parcel: Parcel) {
        this.enabled = parcel.readBooleanCompat()
        this.obfuscationOptions = UnsignedInt(parcel.readInt())
        this.defaultSequence = parcel.readString() ?: defaultSequence
        parcel.readTypedList(this.windowSeqPairs, AutoTypeItem.CREATOR)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeBooleanCompat(enabled)
        dest.writeInt(obfuscationOptions.toKotlinInt())
        dest.writeString(defaultSequence)
        dest.writeTypedList(windowSeqPairs)
    }

    fun add(key: String, value: String) {
        windowSeqPairs.add(AutoTypeItem(key, value))
    }

    fun doForEachAutoTypeItem(action: (key: String, value: String) -> Unit) {
        windowSeqPairs.forEach {
            action.invoke(it.key, it.value)
        }
    }

    private data class AutoTypeItem(var key: String, var value: String): Parcelable {
        constructor(parcel: Parcel) : this(
                parcel.readString() ?: "",
                parcel.readString() ?: "") {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(key)
            parcel.writeString(value)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<AutoTypeItem> {
            override fun createFromParcel(parcel: Parcel): AutoTypeItem {
                return AutoTypeItem(parcel)
            }

            override fun newArray(size: Int): Array<AutoTypeItem?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private val OBF_OPT_NONE = UnsignedInt(0)

        @JvmField
        val CREATOR: Parcelable.Creator<AutoType> = object : Parcelable.Creator<AutoType> {
            override fun createFromParcel(parcel: Parcel): AutoType {
                return AutoType(parcel)
            }

            override fun newArray(size: Int): Array<AutoType?> {
                return arrayOfNulls(size)
            }
        }
    }
}
