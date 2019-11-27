package com.kunzisoft.keepass.database.element

import android.os.Parcel

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

}