package com.kunzisoft.keepass.database.element

import android.os.Parcel

abstract class PwGroup
        <
        Id,
        Group: PwGroupInterface<Group, Entry>,
        Entry: PwEntryInterface<Group>
        >
    : PwNode<Id, Group, Entry>, PwGroupInterface<Group, Entry> {

    private var titleGroup = ""
    @Transient
    private val childGroups = ArrayList<Group>()
    @Transient
    private val childEntries = ArrayList<Entry>()

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        titleGroup = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(titleGroup)
    }

    protected fun updateWith(source: PwGroup<Id, Group, Entry>) {
        super.updateWith(source)
        titleGroup = source.titleGroup
        childGroups.addAll(source.childGroups)
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

    override fun toString(): String {
        return titleGroup
    }
}
