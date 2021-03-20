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

import com.kunzisoft.keepass.utils.ParcelableUtil
import com.kunzisoft.encrypt.UnsignedInt

class AutoType : Parcelable {

    var enabled = true
    var obfuscationOptions = OBF_OPT_NONE
    var defaultSequence = ""
    private var windowSeqPairs = LinkedHashMap<String, String>()

    constructor()

    constructor(autoType: AutoType) {
        this.enabled = autoType.enabled
        this.obfuscationOptions = autoType.obfuscationOptions
        this.defaultSequence = autoType.defaultSequence
        for ((key, value) in autoType.windowSeqPairs) {
            this.windowSeqPairs[key] = value
        }
    }

    constructor(parcel: Parcel) {
        this.enabled = parcel.readByte().toInt() != 0
        this.obfuscationOptions = UnsignedInt(parcel.readInt())
        this.defaultSequence = parcel.readString() ?: defaultSequence
        this.windowSeqPairs = ParcelableUtil.readStringParcelableMap(parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (enabled) 1 else 0).toByte())
        dest.writeInt(obfuscationOptions.toKotlinInt())
        dest.writeString(defaultSequence)
        ParcelableUtil.writeStringParcelableMap(dest, windowSeqPairs)
    }

    fun put(key: String, value: String) {
        windowSeqPairs[key] = value
    }

    fun entrySet(): Set<MutableMap.MutableEntry<String, String>> {
        return windowSeqPairs.entries
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
