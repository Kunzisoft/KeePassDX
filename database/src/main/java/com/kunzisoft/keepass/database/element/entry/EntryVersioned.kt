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
package com.kunzisoft.keepass.database.element.entry

import android.os.Parcel
import com.kunzisoft.keepass.database.element.group.GroupVersioned
import com.kunzisoft.keepass.database.element.node.NodeVersioned

abstract class EntryVersioned
        <
        GroupId,
        EntryId,
        ParentGroup: GroupVersioned<GroupId, EntryId, ParentGroup, Entry>,
        Entry: EntryVersioned<GroupId, EntryId, ParentGroup, Entry>
        >
    : NodeVersioned<EntryId, ParentGroup, Entry>, EntryVersionedInterface<ParentGroup> {

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel)

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    override fun nodeIndexInParentForNaturalOrder(): Int {
        if (nodeIndexInParentForNaturalOrder == -1) {
            val numberOfGroups = parent?.getChildGroups()?.size
            val indexInEntries = parent?.getChildEntries()?.indexOf(this)
            if (numberOfGroups != null && indexInEntries != null)
                return numberOfGroups + indexInEntries
        }
        return nodeIndexInParentForNaturalOrder
    }

}