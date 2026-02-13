package com.kunzisoft.keepass.hardware

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChallengeRequest(
    val hardwareKey: HardwareKey,
    val saveOperation: Boolean = false,
    // TODO Dynamic RP based on package name
    val relyingPartyId: String = "com.kunzisoft.keepass",
    // TODO multiple credentials
    val credentialId: ByteArray?,
    val seed: ByteArray?
): Parcelable{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChallengeRequest

        if (hardwareKey != other.hardwareKey) return false
        if (!credentialId.contentEquals(other.credentialId)) return false
        if (!seed.contentEquals(other.seed)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hardwareKey.hashCode()
        result = 31 * result + (credentialId?.contentHashCode() ?: 0)
        result = 31 * result + (seed?.contentHashCode() ?: 0)
        return result
    }
}
