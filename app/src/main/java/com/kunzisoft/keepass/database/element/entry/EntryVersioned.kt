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

}