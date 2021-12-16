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
import android.os.Parcelable

import java.util.Random

class NodeIdInt : NodeId<Int> {

    override var id: Int = -1
        private set

    constructor(source: NodeIdInt) : this(source.id)

    constructor(groupId: Int = Random().nextInt()) : super() {
        this.id = groupId
    }

    constructor(parcel: Parcel) {
        id = parcel.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (other !is NodeIdInt) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }

    override fun toVisualString(): String? {
        return null
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<NodeIdInt> = object : Parcelable.Creator<NodeIdInt> {
            override fun createFromParcel(parcel: Parcel): NodeIdInt {
                return NodeIdInt(parcel)
            }

            override fun newArray(size: Int): Array<NodeIdInt?> {
                return arrayOfNulls(size)
            }
        }
    }
}
