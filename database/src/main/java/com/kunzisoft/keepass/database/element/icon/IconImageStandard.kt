/*
 * Copyright 2019 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.element.icon

import android.os.Parcel
import android.os.Parcelable

class IconImageStandard : IconImageDraw {

    val id: Int

    constructor() {
        this.id = KEY_ID
    }

    constructor(iconId: Int) {
        if (!isCorrectIconId(iconId))
            this.id = KEY_ID
        else
            this.id = iconId
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + id
        return result
    }

    override fun getIconImageToDraw(): IconImage {
        return IconImage(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is IconImageStandard) {
            return false
        }
        return id == other.id
    }

    companion object {

        const val NUMBER_STANDARD_ICONS = 69

        const val KEY_ID = 0
        const val ID_CARD_ID = 9
        const val WIRELESS_ID = 12
        const val EMAIL_ID = 19
        const val CREDIT_CARD_ID = 37
        const val TRASH_ID = 43
        const val FOLDER_ID = 48
        const val DATABASE_ID = 50
        const val LIST_ID = 57
        const val BUILD_ID = 59
        const val STAR_ID = 61
        const val DOLLAR_ID = 66

        fun isCorrectIconId(iconId: Int): Boolean {
            return iconId in 0 until NUMBER_STANDARD_ICONS
        }

        @JvmField
        val CREATOR: Parcelable.Creator<IconImageStandard> = object : Parcelable.Creator<IconImageStandard> {
            override fun createFromParcel(parcel: Parcel): IconImageStandard {
                return IconImageStandard(parcel)
            }

            override fun newArray(size: Int): Array<IconImageStandard?> {
                return arrayOfNulls(size)
            }
        }
    }
}
