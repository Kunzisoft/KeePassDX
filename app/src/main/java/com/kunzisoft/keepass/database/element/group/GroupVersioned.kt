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
import com.kunzisoft.keepass.database.element.entry.EntryVersioned
import com.kunzisoft.keepass.database.element.node.NodeVersioned
import java.util.*

abstract class GroupVersioned
        <
        GroupId,
        EntryId,
        Group: GroupVersioned<GroupId, EntryId, Group, Entry>,
        Entry: EntryVersioned<GroupId, EntryId, Group, Entry>
        >
    : NodeVersioned<GroupId, Group, Entry>, GroupVersionedInterface<Group, Entry> {

    private var titleGroup = ""
    @Transient
    private val childGroups = LinkedList<Group>()
    @Transient
    private val childEntries = LinkedList<Entry>()
    private var positionIndexChildren = 0

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        titleGroup = parcel.readString() ?: titleGroup
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(titleGroup)
    }

    protected fun updateWith(source: GroupVersioned<GroupId, EntryId, Group, Entry>,
                             updateParents: Boolean = true) {
        super.updateWith(source, updateParents)
        titleGroup = source.titleGroup
        if (updateParents) {
            removeChildren()
            childGroups.addAll(source.childGroups)
            childEntries.addAll(source.childEntries)
        }
    }

    override var title: String
        get() = titleGroup
        set(value) { titleGroup = value }

    /**
     *  To determine the level from the root group (root group level is -1)
     */
    fun getLevel(): Int {
        var level = -1
        parent?.let { parent ->
            level = parent.getLevel() + 1
        }
        return level
    }

    override fun getChildGroups(): List<Group> {
        return childGroups
    }

    override fun getChildEntries(): List<Entry> {
        return childEntries
    }

    override fun addChildGroup(group: Group) {
        if (childGroups.contains(group))
            removeChildGroup(group)
        positionIndexChildren++
        group.nodeIndexInParentForNaturalOrder = positionIndexChildren
        this.childGroups.add(group)
    }

    override fun addChildEntry(entry: Entry) {
        if (childEntries.contains(entry))
            removeChildEntry(entry)
        positionIndexChildren++
        entry.nodeIndexInParentForNaturalOrder = positionIndexChildren
        this.childEntries.add(entry)
    }

    override fun updateChildGroup(group: Group) {
        val index = this.childGroups.indexOfFirst { it.nodeId == group.nodeId }
        if (index >= 0) {
            val oldGroup = this.childGroups.removeAt(index)
            group.nodeIndexInParentForNaturalOrder = oldGroup.nodeIndexInParentForNaturalOrder
            this.childGroups.add(index, group)
        }
    }

    override fun updateChildEntry(entry: Entry) {
        val index = this.childEntries.indexOfFirst { it.nodeId == entry.nodeId }
        if (index >= 0) {
            val oldEntry = this.childEntries.removeAt(index)
            entry.nodeIndexInParentForNaturalOrder = oldEntry.nodeIndexInParentForNaturalOrder
            this.childEntries.add(index, entry)
        }
    }

    override fun removeChildGroup(group: Group) {
        this.childGroups.remove(group)
    }

    override fun removeChildEntry(entry: Entry) {
        this.childEntries.remove(entry)
    }

    override fun removeChildren() {
        this.childGroups.clear()
        this.childEntries.clear()
    }

    override fun nodeIndexInParentForNaturalOrder(): Int {
        return if (nodeIndexInParentForNaturalOrder == -1)
            childGroups.indexOf(this)
        else
            nodeIndexInParentForNaturalOrder
    }
}
