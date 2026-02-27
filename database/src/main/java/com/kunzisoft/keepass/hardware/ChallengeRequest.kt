package com.kunzisoft.keepass.hardware

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChallengeRequest(
    val hardwareKey: HardwareKey,
    val operation: ChallengeOperation = ChallengeOperation.GET,
    val relyingPartyId: String,
    val credentials: List<ByteArray>,
    val seed: ByteArray?,
    val seedOptional: ByteArray? = null,
    val clientData: ByteArray? = null
): Parcelable {

    enum class ChallengeOperation {
        CREATE, UPDATE, GET
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChallengeRequest

        if (hardwareKey != other.hardwareKey) return false
        if (operation != other.operation) return false
        if (relyingPartyId != other.relyingPartyId) return false
        if (credentials != other.credentials) return false
        if (!seed.contentEquals(other.seed)) return false
        if (!seedOptional.contentEquals(other.seedOptional)) return false
        if (!clientData.contentEquals(other.clientData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hardwareKey.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + relyingPartyId.hashCode()
        result = 31 * result + credentials.hashCode()
        result = 31 * result + (seed?.contentHashCode() ?: 0)
        result = 31 * result + (seedOptional?.contentHashCode() ?: 0)
        result = 31 * result + (clientData?.contentHashCode() ?: 0)
        return result
    }
}
