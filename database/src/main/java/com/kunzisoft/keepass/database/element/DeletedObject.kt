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
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.utils.readParcelableCompat
import java.util.*

class DeletedObject : Parcelable {

    var uuid: UUID = DatabaseVersioned.UUID_ZERO
    var deletionTime: DateInstant = DateInstant()

    constructor()

    constructor(uuid: UUID, deletionTime: DateInstant = DateInstant()) {
        this.uuid = uuid
        this.deletionTime = deletionTime
    }

    constructor(parcel: Parcel) {
        uuid = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: DatabaseVersioned.UUID_ZERO
        deletionTime = parcel.readParcelableCompat() ?: deletionTime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is DeletedObject)
            return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(ParcelUuid(uuid), flags)
        parcel.writeParcelable(deletionTime, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeletedObject> {
        override fun createFromParcel(parcel: Parcel): DeletedObject {
            return DeletedObject(parcel)
        }

        override fun newArray(size: Int): Array<DeletedObject?> {
            return arrayOfNulls(size)
        }
    }
}
