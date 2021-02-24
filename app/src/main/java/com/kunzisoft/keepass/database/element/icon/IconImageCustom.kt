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
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import java.util.*

class IconImageCustom() : Parcelable {

    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    @Transient
    var imageData: ByteArray = ByteArray(0)

    constructor(uuid: UUID, data: ByteArray) : this() {
        this.uuid = uuid
        this.imageData = data
    }

    constructor(uuid: UUID) : this() {
        this.uuid = uuid
        this.imageData = ByteArray(0)
    }

    constructor(parcel: Parcel) : this() {
        uuid = parcel.readSerializable() as UUID
        // TODO Take too much memories
        // parcel.readByteArray(imageData);
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(uuid)
        // Too big for a parcelable dest.writeByteArray(imageData);
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + uuid.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is IconImageCustom)
            return false
        return uuid == other.uuid
    }

    val isUnknown: Boolean
        get() = uuid == DatabaseVersioned.UUID_ZERO

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<IconImageCustom> = object : Parcelable.Creator<IconImageCustom> {
            override fun createFromParcel(parcel: Parcel): IconImageCustom {
                return IconImageCustom(parcel)
            }

            override fun newArray(size: Int): Array<IconImageCustom?> {
                return arrayOfNulls(size)
            }
        }
    }
}
