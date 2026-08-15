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
import com.kunzisoft.keepass.database.crypto.kdf.KdfParameters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class KdfClampingTest {

    @Test
    fun testArgon2KdfClamping() {
        val argon2Kdf = Argon2Kdf(Argon2Kdf.Type.ARGON2_ID)

        // Test iterations clamping
        argon2Kdf.setKeyRounds(2_000_000L) // Above max
        assertEquals(1_000_000L, argon2Kdf.getKeyRounds())

        argon2Kdf.setKeyRounds(0L) // Below min
        assertEquals(1L, argon2Kdf.getKeyRounds())

        // Test memory clamping
        argon2Kdf.setMemoryUsage( 5_000_000_000L) // Above max (4GiB)
        assertEquals(4_294_967_295L, argon2Kdf.getMemoryUsage())

        argon2Kdf.setMemoryUsage( 100L) // Below min
        assertEquals(8192L, argon2Kdf.getMemoryUsage())

        // Test parallelism clamping
        argon2Kdf.setParallelism(256L) // Above max
        assertEquals(128L, argon2Kdf.getParallelism())

        argon2Kdf.setParallelism(0L) // Below min
        assertEquals(1L, argon2Kdf.getParallelism())
    }

    @Test
    fun testAesKdfClamping() {
        val aesKdf = AesKdf()

        // Test rounds clamping
        aesKdf.setKeyRounds(200_000_000L) // Above max
        assertEquals(100_000_000L, aesKdf.getKeyRounds())

        aesKdf.setKeyRounds(0L) // Below min
        assertEquals(1L, aesKdf.getKeyRounds())
    }
}
