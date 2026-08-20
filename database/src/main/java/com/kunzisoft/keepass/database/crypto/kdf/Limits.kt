/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.crypto.kdf

import android.os.Parcelable
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.MAX_BINARY_BYTE
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * Class to calculate fresh local resource limits for memory operations.
 * It provides a mechanism to check if a specific memory allocation is safe based on the device's
 * hardware capabilities and current resource usage.
 *
 * @property isMemorySufficient Function that determines if a requested memory allocation is safe
 * given the operation type.
 * @property parallelism The number of available processors for parallel operations.
 */
@Parcelize
data class Limits(
    val isMemorySufficient: (memoryWanted: ULong, type: LimitOperationType) -> Boolean,
    val parallelism: Long
) : Parcelable, Serializable {

    /**
     * Defines the type of operation requiring memory and its associated safety constraints.
     *
     * @property maxMemory The absolute maximum memory allowed for this operation type.
     * @property ratioAvailableMemory The maximum fraction of available system memory that can be safely used.
     */
    enum class LimitOperationType(val maxMemory: ULong, val ratioAvailableMemory: Float) {
        // Operation involving large binary data
        BINARY(maxMemory = MAX_BINARY_BYTE, 1f/5),
        // Operation for Key Derivation Function (KDF) like Argon2
        KDF(maxMemory = UInt.MAX_VALUE.toULong(), 1f/2)
    }

    /**
     * Helper function to check if memory allocation for a binary element is safe.
     *
     * @param memoryWanted Requested memory size in bytes.
     * @return True if the allocation is within safe limits, false otherwise.
     */
    fun isMemorySufficientForBinary(memoryWanted: ULong): Boolean {
        return isMemorySufficient(
            memoryWanted,
            LimitOperationType.BINARY
        )
    }
}