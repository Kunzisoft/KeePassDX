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

class PwGroupV3 : PwGroup<Int, PwGroupV3, PwEntryV3> {

    var level = 0 // short
    /** Used by KeePass internally, don't use  */
    var flags: Int = 0

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        level = parcel.readInt()
        flags = parcel.readInt()
    }

    override fun readParentParcelable(parcel: Parcel): PwGroupV3 {
        return parcel.readParcelable(PwGroupV3::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(level)
        dest.writeInt(flags)
    }

    fun updateWith(source: PwGroupV3) {
        super.updateWith(source)
        level = source.level
        flags = source.flags
    }

    override val type: Type
        get() = Type.GROUP

    override val isSearchingEnabled: Boolean
        get() = false

    override fun initNodeId(): PwNodeId<Int> {
        return PwNodeIdInt()
    }

    override fun copyNodeId(nodeId: PwNodeId<Int>): PwNodeId<Int> {
        return PwNodeIdInt(nodeId.id)
    }

    override fun afterAssignNewParent() {
        if (parent != null)
            level = parent!!.level + 1
    }

    fun setGroupId(groupId: Int) {
        this.nodeId = PwNodeIdInt(groupId)
    }

    override fun allowAddEntryIfIsRoot(): Boolean {
        return false
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<PwGroupV3> = object : Parcelable.Creator<PwGroupV3> {
            override fun createFromParcel(parcel: Parcel): PwGroupV3 {
                return PwGroupV3(parcel)
            }

            override fun newArray(size: Int): Array<PwGroupV3?> {
                return arrayOfNulls(size)
            }
        }
    }
}
