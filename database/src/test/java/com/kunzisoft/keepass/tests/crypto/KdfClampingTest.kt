/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePassDX. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.tests.crypto

import com.kunzisoft.keepass.database.crypto.kdf.AesKdf
import com.kunzisoft.keepass.database.crypto.kdf.Argon2Kdf
import com.kunzisoft.keepass.database.crypto.kdf.Limits
import org.junit.Assert.assertEquals
import org.junit.Test

class KdfClampingTest {

    @Test
    fun testArgon2KdfClamping() {
        val argon2Kdf = Argon2Kdf(Argon2Kdf.Type.ARGON2_ID)

        // Test iterations clamping
        argon2Kdf.setKeyRounds(5_000_000_000uL) // Above max
        assertEquals(argon2Kdf.maxKeyRounds, argon2Kdf.getKeyRounds())

        argon2Kdf.setKeyRounds(0uL) // Below min
        assertEquals(argon2Kdf.minKeyRounds, argon2Kdf.getKeyRounds())

        // Test memory clamping with limits
        argon2Kdf.setMemoryUsage(5_000_000_000uL) // Above max (4GiB)
        assertEquals(argon2Kdf.maxMemoryUsage, argon2Kdf.getMemoryUsage())

        argon2Kdf.setMemoryUsage(100uL) // Below min
        assertEquals(argon2Kdf.minMemoryUsage, argon2Kdf.getMemoryUsage())

        // Test parallelism clamping with limits
        argon2Kdf.setParallelism(20_000_000L) // Above max
        assertEquals(argon2Kdf.maxParallelism, argon2Kdf.getParallelism())

        argon2Kdf.setParallelism(0L) // Below min
        assertEquals(argon2Kdf.minParallelism, argon2Kdf.getParallelism())

        // Test getter clamping from raw parameters (simulating malicious file)
        argon2Kdf.parameters.setUInt64("M", 5_000_000_000uL)
        assertEquals(argon2Kdf.maxMemoryUsage, argon2Kdf.getMemoryUsage()) // Clamped by limits
    }

    @Test
    fun testArgon2KdfException() {
        Argon2Kdf(Argon2Kdf.Type.ARGON2_ID).apply {
            setParallelism(4L)
            setMemoryUsage(5_000_000_000uL)
            try {
                checkLimits(
                    Limits(
                        isMemorySufficient = { memoryWanted, _ ->
                            memoryWanted < 16_000_000uL
                        },
                        parallelism = 4L
                    )
                )
                assert(false)
            } catch (_: SecurityException) {
                assert(true)
            }
        }
    }

    @Test
    fun testAesKdfClamping() {
        val aesKdf = AesKdf()

        // Test rounds clamping
        aesKdf.setKeyRounds(10_000_000_000_000_000_000uL) // Above max
        assertEquals(aesKdf.maxKeyRounds, aesKdf.getKeyRounds())

        aesKdf.setKeyRounds(0uL) // Below min
        assertEquals(aesKdf.minKeyRounds, aesKdf.getKeyRounds())
    }

    @Test(expected = SecurityException::class)
    fun testAesKdfRoundsException() {
        val aesKdf = AesKdf()
        aesKdf.parameters.setUInt64("R", 10_000_000_000_000_000_000uL) // Above max
        aesKdf.checkLimits(
            Limits(
                isMemorySufficient = { _, _ -> true },
                parallelism = 4L
            )
        )
    }
}
