package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.security.BinaryAttachment

data class EntryAttachment(var name: String, var binaryAttachment: BinaryAttachment)