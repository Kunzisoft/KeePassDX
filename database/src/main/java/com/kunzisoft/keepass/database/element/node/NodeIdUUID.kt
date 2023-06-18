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
package com.kunzisoft.keepass.database.element.node

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.UuidUtil
import java.util.*

class NodeIdUUID : NodeId<UUID> {

    override var id: UUID = UUID.randomUUID()
        private set

    constructor(source: NodeIdUUID) : this(source.id)

    constructor(uuid: UUID = UUID.randomUUID()) : super() {
        this.id = uuid
    }

    constructor(parcel: Parcel) {
        id = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: id
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeParcelable(ParcelUuid(id), flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is NodeIdUUID) {
            return false
        }
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return UuidUtil.toHexString(id) ?: id.toString()
    }

    override fun toVisualString(): String {
        return toString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NodeIdUUID> = object : Parcelable.Creator<NodeIdUUID> {
            override fun createFromParcel(parcel: Parcel): NodeIdUUID {
                return NodeIdUUID(parcel)
            }

            override fun newArray(size: Int): Array<NodeIdUUID?> {
                return arrayOfNulls(size)
            }
        }
    }
}
