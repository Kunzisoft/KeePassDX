package com.kunzisoft.keepass.database.element

import android.os.Parcel
import java.util.*

abstract class PwEntry
        <
        ParentGroup: PwGroupInterface<ParentGroup, Entry>,
        Entry: PwEntryInterface<ParentGroup>
        >
    : PwNode<UUID, ParentGroup, Entry>, PwEntryInterface<ParentGroup> {

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel)

}