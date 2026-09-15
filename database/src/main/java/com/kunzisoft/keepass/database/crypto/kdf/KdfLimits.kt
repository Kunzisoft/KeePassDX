package com.kunzisoft.keepass.database.crypto.kdf

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Data model for local device resource limits and hardware resource availability.
 */
@Parcelize
data class KdfLimits(
    val memory: ULong,
    val parallelism: Long
) : Parcelable, Serializable