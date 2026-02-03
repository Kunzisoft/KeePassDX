package com.kunzisoft.encrypt

import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Argon2Test {

    @Test
    fun testArgon2NativeTransform() {
        val password = "password".toByteArray()
        val salt = "saltsaltsaltsalt".toByteArray() // 16 bytes

        val iterations = 2L
        val memory = 1024L * 64 // 64 MB
        val parallelism = 2L
        val version = 0x13 // Argon2 v1.3

        val hash1 = Argon2Transformer.transformKey(
            type = Argon2Type.ARGON2_I,
            password = password,
            salt = salt,
            parallelism = parallelism,
            memory = memory,
            iterations = iterations,
            version = version
        )
        assertArrayEquals("Hashing same data should produce same result",
            hash1,
            byteArrayOf(117, -12, 108, 66, -5, 112, 41, -126, 110, 97, 16, -87, -101, 39, 60, -69, 74, -118, 28, -124, -9, -90, 34, 31, -94, -104, -36, -82, -72, 20, -17, 13)
        )

        val hash2 = Argon2Transformer.transformKey(
            type = Argon2Type.ARGON2_D,
            password = password,
            salt = salt,
            parallelism = parallelism,
            memory = memory,
            iterations = iterations,
            version = version
        )
        assertArrayEquals("Hashing same data should produce same result",
            hash2,
            byteArrayOf(42, 112, 71, -4, -1, -22, 7, -25, 113, 75, -61, -18, -91, 108, 2, -70, -115, 14, -112, 70, -45, -61, -94, 58, 40, -124, 43, -94, -90, -15, 30, 72)
        )

        val hash3 = Argon2Transformer.transformKey(
            type = Argon2Type.ARGON2_ID,
            password = password,
            salt = salt,
            parallelism = parallelism,
            memory = memory,
            iterations = iterations,
            version = version
        )
        assertArrayEquals("Hashing same data should produce same result",
            hash3,
            byteArrayOf(-74, -16, 14, -62, -79, 70, 44, 6, 91, 46, 85, 97, 90, 47, 67, 36, -61, 96, -45, -35, -59, 16, -104, 101, 5, -63, -53, -85, 116, -116, -18, 47)
        )
    }
}