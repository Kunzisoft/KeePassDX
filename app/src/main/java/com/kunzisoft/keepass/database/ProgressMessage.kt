package com.kunzisoft.keepass.database

data class ProgressMessage(
    var title: String,
    var message: String? = null,
    var warning: String? = null,
    var cancelable: (() -> Unit)? = null
)
