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
 *
 */
package com.kunzisoft.keepass.database.element.icon

import android.os.Parcel
import android.os.Parcelable

class IconImage() : Parcelable {

    var standard: IconImageStandard = IconImageStandard()
    var custom: IconImageCustom = IconImageCustom()

    constructor(iconImageStandard: IconImageStandard,
                iconImageCustom: IconImageCustom = IconImageCustom()) : this() {
        this.standard = iconImageStandard
        this.custom = iconImageCustom
    }

    constructor(parcel: Parcel) : this() {
        standard = parcel.readParcelable(IconImageStandard::class.java.classLoader) ?: standard
        custom = parcel.readParcelable(IconImageCustom::class.java.classLoader) ?: custom
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(standard, flags)
        parcel.writeParcelable(custom, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<IconImage> {
        override fun createFromParcel(parcel: Parcel): IconImage {
            return IconImage(parcel)
        }

        override fun newArray(size: Int): Array<IconImage?> {
            return arrayOfNulls(size)
        }
    }
}
