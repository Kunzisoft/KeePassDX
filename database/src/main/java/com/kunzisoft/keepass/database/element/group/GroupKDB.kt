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

package com.kunzisoft.keepass.database.element.group

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.entry.EntryKDB
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeKDBInterface
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.utils.readParcelableCompat
import java.util.*

class GroupKDB : GroupVersioned<Int, UUID, GroupKDB, EntryKDB>, NodeKDBInterface {

    // Used by KeePass internally, don't use
    var groupFlags = 0

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        groupFlags = parcel.readInt()
    }

    override fun readParentParcelable(parcel: Parcel): GroupKDB? {
        return parcel.readParcelableCompat()
    }

    override fun writeParentParcelable(parent: GroupKDB?, parcel: Parcel, flags: Int) {
        parcel.writeParcelable(parent, flags)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(groupFlags)
    }

    fun updateWith(source: GroupKDB,
                   updateParents: Boolean = true) {
        super.updateWith(source, updateParents)
        groupFlags = source.groupFlags
    }

    override val type: Type
        get() = Type.GROUP

    override fun initNodeId(): NodeId<Int> {
        return NodeIdInt()
    }

    override fun copyNodeId(nodeId: NodeId<Int>): NodeId<Int> {
        return NodeIdInt(nodeId.id)
    }

    fun setGroupId(groupId: Int) {
        this.nodeId = NodeIdInt(groupId)
    }

    override fun afterAssignNewParent() {}

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<GroupKDB> = object : Parcelable.Creator<GroupKDB> {
            override fun createFromParcel(parcel: Parcel): GroupKDB {
                return GroupKDB(parcel)
            }

            override fun newArray(size: Int): Array<GroupKDB?> {
                return arrayOfNulls(size)
            }
        }
    }
}
