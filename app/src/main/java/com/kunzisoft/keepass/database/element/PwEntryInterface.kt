package com.kunzisoft.keepass.database.element

interface PwEntryInterface<ParentGroup> : PwNodeInterface<ParentGroup> {

    var username: String

    var password: String

    var url: String

    var notes: String
}
