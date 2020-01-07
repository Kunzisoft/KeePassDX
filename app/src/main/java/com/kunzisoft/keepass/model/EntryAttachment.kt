package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.security.BinaryAttachment

data class EntryAttachment(var name: String,
                           var binaryAttachment: BinaryAttachment,
                           var downloadInProgress: Boolean = false,
                           var downloadProgression: Int = 0)