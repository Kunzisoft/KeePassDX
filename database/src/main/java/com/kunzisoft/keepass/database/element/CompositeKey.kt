package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.clear

data class CompositeKey(
    var passwordData: ByteArray? = null,
    var keyFileData: ByteArray? = null,
    var hardwareKey: HardwareKey? = null
) {

    /**
     * Build the master key from the composite key parts.
     * @param transformSeed The transform seed for the hardware key challenge.
     * @param challengeResponseRetriever The hardware key challenge retriever.
     * @return The SHA-256 hash of all key parts.
     */
    fun toMasterKey(
        transformSeed: ByteArray?,
        challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray
    ): ByteArray {
        val hardwareKeyData = hardwareKey?.let {
            MasterCredential.retrieveHardwareKey(challengeResponseRetriever.invoke(it, transformSeed))
        }

        val masterKey = MasterCredential.composedKeyToMasterKey(
            passwordData = passwordData,
            keyFileData = keyFileData,
            hardwareKeyData = hardwareKeyData
        )

        hardwareKeyData?.clear()
        return masterKey
    }

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

    fun copyOf(): CompositeKey {
        return CompositeKey(
            passwordData?.copyOf(),
            keyFileData?.copyOf(),
            hardwareKey
        )
    }

    fun clear() {
        passwordData?.clear()
        keyFileData?.clear()
        passwordData = null
        keyFileData = null
        hardwareKey = null
    }
}