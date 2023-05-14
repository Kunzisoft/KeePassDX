package com.kunzisoft.keepass.database

import androidx.annotation.StringRes

data class ProgressMessage(
    @StringRes
    var titleId: Int,
    @StringRes
    var messageId: Int? = null,
    @StringRes
    var warningId: Int? = null,
    var cancelable: (() -> Unit)? = null
)
