package com.kunzisoft.encrypt

import android.os.Build

object NativeBlockList {
    val isBlocked: Boolean by lazy {
        Build.MODEL == "A500"
    }
}