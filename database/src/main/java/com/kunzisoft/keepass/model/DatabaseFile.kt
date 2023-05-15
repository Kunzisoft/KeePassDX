package com.kunzisoft.keepass.model

import android.net.Uri
import com.kunzisoft.keepass.hardware.HardwareKey

data class DatabaseFile(var databaseUri: Uri? = null,
                        var keyFileUri: Uri? = null,
                        var hardwareKey: HardwareKey? = null,
                        var databaseDecodedPath: String? = null,
                        var databaseAlias: String? = null,
                        var databaseFileExists: Boolean = false,
                        var databaseLastModified: String? = null,
                        var databaseSize: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DatabaseFile) return false

        if (databaseUri == null || other.databaseUri == null) return false
        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri?.hashCode() ?: 0
    }
}