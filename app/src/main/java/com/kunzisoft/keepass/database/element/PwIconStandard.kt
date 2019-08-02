/*
 * Copyright 2019 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable

class PwIconStandard : PwIcon {

    constructor() {
        this.iconId = KEY
    }

    constructor(iconId: Int) {
        this.iconId = iconId
    }

    constructor(icon: PwIconStandard) {
        this.iconId = icon.iconId
    }

    constructor(parcel: Parcel) {
        iconId = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(iconId)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + iconId
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is PwIconStandard) {
            return false
        }
        return iconId == other.iconId
    }

    override val iconId: Int

    override val isUnknown: Boolean
        get() = iconId == UNKNOWN_ID

    override val isMetaStreamIcon: Boolean
        get() = iconId == 0

    companion object {

        const val KEY = 0
        const val TRASH = 43
        const val FOLDER = 48

        @JvmField
        val CREATOR: Parcelable.Creator<PwIconStandard> = object : Parcelable.Creator<PwIconStandard> {
            override fun createFromParcel(parcel: Parcel): PwIconStandard {
                return PwIconStandard(parcel)
            }

            override fun newArray(size: Int): Array<PwIconStandard?> {
                return arrayOfNulls(size)
            }
        }
    }
}
