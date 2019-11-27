package com.kunzisoft.keepass.database.element.group

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface

interface GroupVersionedInterface<Group: GroupVersionedInterface<Group, Entry>, Entry> : NodeVersionedInterface<Group> {

    fun getChildGroups(): MutableList<Group>

    fun getChildEntries(): MutableList<Entry>

    fun addChildGroup(group: Group)

    fun addChildEntry(entry: Entry)

    fun removeChildGroup(group: Group)

    fun removeChildEntry(entry: Entry)

    fun removeChildren()

    fun allowAddEntryIfIsRoot(): Boolean

    fun doForEachChildAndForIt(entryHandler: NodeHandler<Entry>,
                               groupHandler: NodeHandler<Group>) {
        doForEachChild(entryHandler, groupHandler)
        groupHandler.operate(this as Group)
    }

    fun doForEachChild(entryHandler: NodeHandler<Entry>,
                       groupHandler: NodeHandler<Group>?,
                       stopIterationWhenGroupHandlerFails: Boolean = true): Boolean {
        for (entry in this.getChildEntries()) {
            if (!entryHandler.operate(entry))
                return false
        }
        for (group in this.getChildGroups()) {
            var doActionForChild = true
            if (groupHandler != null && !groupHandler.operate(group)) {
                doActionForChild = false
                if (stopIterationWhenGroupHandlerFails)
                    return false
            }
            if (doActionForChild)
                group.doForEachChild(entryHandler, groupHandler)
        }
        return true
    }
}
