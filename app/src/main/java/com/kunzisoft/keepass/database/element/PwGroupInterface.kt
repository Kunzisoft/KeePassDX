package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.database.NodeHandler

interface PwGroupInterface<Group: PwGroupInterface<Group, Entry>, Entry> : PwNodeInterface<Group> {

    fun getChildGroups(): MutableList<Group>

    fun getChildEntries(): MutableList<Entry>

    fun addChildGroup(group: Group)

    fun addChildEntry(entry: Entry)

    fun removeChildGroup(group: Group)

    fun removeChildEntry(entry: Entry)

    fun allowAddEntryIfIsRoot(): Boolean

    fun doForEachChildAndForIt(entryHandler: NodeHandler<Entry>,
                               groupHandler: NodeHandler<Group>) {
        doForEachChild(entryHandler, groupHandler)
        groupHandler.operate(this as Group)
    }

    fun doForEachChild(entryHandler: NodeHandler<Entry>,
                       groupHandler: NodeHandler<Group>?): Boolean {
        for (entry in this.getChildEntries()) {
            if (!entryHandler.operate(entry)) return false
        }
        for (group in this.getChildGroups()) {
            if (groupHandler != null && !groupHandler.operate(group)) return false
            group.doForEachChild(entryHandler, groupHandler)
        }
        return true
    }
}
