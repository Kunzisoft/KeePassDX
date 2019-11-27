package com.kunzisoft.keepass.database.element

import android.os.Parcel

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
    private val childGroups = ArrayList<Group>()
    @Transient
    private val childEntries = ArrayList<Entry>()

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        titleGroup = parcel.readString() ?: titleGroup
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(titleGroup)
    }

    protected fun updateWith(source: GroupVersioned<GroupId, EntryId, Group, Entry>) {
        super.updateWith(source)
        titleGroup = source.titleGroup
        childGroups.clear()
        childGroups.addAll(source.childGroups)
        childEntries.clear()
        childEntries.addAll(source.childEntries)
    }

    override var title: String
        get() = titleGroup
        set(value) { titleGroup = value }

    override fun getChildGroups(): MutableList<Group> {
        return childGroups
    }

    override fun getChildEntries(): MutableList<Entry> {
        return childEntries
    }

    override fun addChildGroup(group: Group) {
        if (childGroups.contains(group))
            removeChildGroup(group)
        this.childGroups.add(group)
    }

    override fun addChildEntry(entry: Entry) {
        if (childEntries.contains(entry))
            removeChildEntry(entry)
        this.childEntries.add(entry)
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

    override fun toString(): String {
        return titleGroup
    }
}
