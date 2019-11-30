package com.kunzisoft.keepass.database.element.entry

import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface

interface EntryVersionedInterface<ParentGroup> : NodeVersionedInterface<ParentGroup> {

    var username: String

    var password: String

    var url: String

    var notes: String
}
