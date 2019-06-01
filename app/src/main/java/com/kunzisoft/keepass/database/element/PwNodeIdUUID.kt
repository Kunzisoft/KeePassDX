/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import java.util.UUID

class PwNodeIdUUID : PwNodeId<UUID> {

    override var id: UUID = UUID.randomUUID()
        private set

    constructor(source: PwNodeIdUUID) : this(source.id)

    @JvmOverloads
    constructor(uuid: UUID = UUID.randomUUID()) : super() {
        this.id = uuid
    }

    constructor(parcel: Parcel) {
        id = parcel.readSerializable() as UUID
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeSerializable(id)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PwNodeIdUUID) {
            return false
        }
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PwNodeIdUUID> = object : Parcelable.Creator<PwNodeIdUUID> {
            override fun createFromParcel(`in`: Parcel): PwNodeIdUUID {
                return PwNodeIdUUID(`in`)
            }

            override fun newArray(size: Int): Array<PwNodeIdUUID?> {
                return arrayOfNulls(size)
            }
        }
    }
}
