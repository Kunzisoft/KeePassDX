package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.hardware.HardwareKey

data class CompositeKey(var passwordData: ByteArray? = null,
                        var keyFileData: ByteArray? = null,
                        var hardwareKey: HardwareKey? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompositeKey

        if (passwordData != null) {
            if (other.passwordData == null) return false
            if (!passwordData.contentEquals(other.passwordData)) return false
        } else if (other.passwordData != null) return false
        if (keyFileData != null) {
            if (other.keyFileData == null) return false
            if (!keyFileData.contentEquals(other.keyFileData)) return false
        } else if (other.keyFileData != null) return false
        if (hardwareKey != other.hardwareKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = passwordData?.contentHashCode() ?: 0
        result = 31 * result + (keyFileData?.contentHashCode() ?: 0)
        result = 31 * result + (hardwareKey?.hashCode() ?: 0)
        return result
    }
}