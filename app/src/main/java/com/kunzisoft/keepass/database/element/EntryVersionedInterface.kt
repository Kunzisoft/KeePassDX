package com.kunzisoft.keepass.database.element

interface EntryVersionedInterface<ParentGroup> : NodeVersionedInterface<ParentGroup> {

    var username: String

    var password: String

    var url: String

    var notes: String
}
