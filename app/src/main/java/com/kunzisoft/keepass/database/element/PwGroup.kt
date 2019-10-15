package com.kunzisoft.keepass.database.element

import android.os.Parcel

abstract class PwGroup
        <
        GroupId,
        EntryId,
        Group: PwGroup<GroupId, EntryId, Group, Entry>,
        Entry: PwEntry<GroupId, EntryId, Group, Entry>
        >
    : PwNode<GroupId, Group, Entry>, PwGroupInterface<Group, Entry> {

    private var titleGroup = ""
    @Transient
    private val childGroups = LinkedHashMap<PwNodeId<GroupId>, Group>()
    @Transient
    private val childEntries = LinkedHashMap<PwNodeId<EntryId>, Entry>()

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        titleGroup = parcel.readString() ?: titleGroup
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(titleGroup)
    }

    protected fun updateWith(source: PwGroup<GroupId, EntryId, Group, Entry>) {
        super.updateWith(source)
        titleGroup = source.titleGroup
        childGroups.clear()
        childGroups.putAll(source.childGroups)
        childEntries.clear()
        childEntries.putAll(source.childEntries)
    }

    override var title: String
        get() = titleGroup
        set(value) { titleGroup = value }

    override fun getChildGroups(): MutableList<Group> {
        return childGroups.values.toMutableList()
    }

    override fun getChildEntries(): MutableList<Entry> {
        return childEntries.values.toMutableList()
    }

    override fun addChildGroup(group: Group) {
        this.childGroups[group.nodeId] = group
    }

    override fun addChildEntry(entry: Entry) {
        this.childEntries[entry.nodeId] = entry
    }

    override fun updateChildEntry(entry: Entry) {
        this.childEntries[entry.nodeId] = entry
    }

    override fun updateChildGroup(group: Group) {
        this.childGroups[group.nodeId] = group
    }

    override fun removeChildGroup(group: Group) {
        this.childGroups.remove(group.nodeId)
    }

    override fun removeChildEntry(entry: Entry) {
        this.childEntries.remove(entry.nodeId)
    }

    override fun toString(): String {
        return titleGroup
    }
}
