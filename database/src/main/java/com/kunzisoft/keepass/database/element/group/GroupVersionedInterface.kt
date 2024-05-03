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

import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface

interface GroupVersionedInterface<Group: GroupVersionedInterface<Group, Entry>, Entry> : NodeVersionedInterface<Group> {

    fun getChildGroups(): List<Group>

    fun getChildEntries(): List<Entry>

    fun addChildGroup(group: Group)

    fun addChildEntry(entry: Entry)

    fun updateChildGroup(group: Group)

    fun updateChildEntry(entry: Entry)

    fun removeChildGroup(group: Group)

    fun removeChildEntry(entry: Entry)

    fun removeChildren()

    @Suppress("UNCHECKED_CAST")
    fun doForEachChildAndForIt(entryHandler: NodeHandler<Entry>,
                               groupHandler: NodeHandler<Group>) {
        doForEachChild(entryHandler, groupHandler)
        groupHandler.operate(this as Group)
    }

    fun doForEachChild(entryHandler: NodeHandler<Entry>?,
                       groupHandler: NodeHandler<Group>?,
                       stopIterationWhenGroupHandlerOperateFalse: Boolean = true): Boolean {
        if (entryHandler != null) {
            for (entry in this.getChildEntries()) {
                if (!entryHandler.operate(entry))
                    return false
            }
        }
        for (group in this.getChildGroups()) {
            var doActionForChild = true
            if (groupHandler != null && !groupHandler.operate(group)) {
                doActionForChild = false
                if (stopIterationWhenGroupHandlerOperateFalse)
                    return false
            }
            if (doActionForChild)
                group.doForEachChild(entryHandler, groupHandler, stopIterationWhenGroupHandlerOperateFalse)
        }
        return true
    }

    fun searchChildEntry(criteria: (entry: Entry) -> Boolean): Entry? {
        return searchChildEntry(this, criteria)
    }

    private fun searchChildEntry(rootGroup: GroupVersionedInterface<Group, Entry>,
                                 criteria: (entry: Entry) -> Boolean): Entry? {
        for (childEntry in rootGroup.getChildEntries()) {
            if (criteria.invoke(childEntry)) {
                return childEntry
            }
        }
        for (group in rootGroup.getChildGroups()) {
            val searchChildEntry = searchChildEntry(group, criteria)
            if (searchChildEntry != null) {
                return searchChildEntry
            }
        }
        return null
    }

    fun searchChildGroup(criteria: (group: Group) -> Boolean): Group? {
        return searchChildGroup(this, criteria)
    }

    private fun searchChildGroup(rootGroup: GroupVersionedInterface<Group, Entry>,
                                 criteria: (group: Group) -> Boolean): Group? {
        for (childGroup in rootGroup.getChildGroups()) {
            if (criteria.invoke(childGroup)) {
                return childGroup
            } else {
                val subGroup = searchChildGroup(childGroup, criteria)
                if (subGroup != null) {
                    return subGroup
                }
            }
        }
        return null
    }
}
