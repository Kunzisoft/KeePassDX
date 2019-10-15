package com.kunzisoft.keepass.database.element

import android.os.Parcel
import java.util.*

abstract class PwEntry
        <
        GroupId,
        EntryId,
        ParentGroup: PwGroup<GroupId, EntryId, ParentGroup, Entry>,
        Entry: PwEntry<GroupId, EntryId, ParentGroup, Entry>
        >
    : PwNode<EntryId, ParentGroup, Entry>, PwEntryInterface<ParentGroup> {

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel)

}