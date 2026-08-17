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

import com.kunzisoft.keepass.database.crypto.VariantDictionary
import com.kunzisoft.keepass.database.crypto.kdf.AesKdf
import com.kunzisoft.keepass.database.crypto.kdf.Argon2Kdf
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class KdfSerializationTest {

    @Test
    fun testVariantDictionarySerialization() {
        val vd = VariantDictionary()
        vd.setUInt32("u32", 1234u)
        vd.setUInt64("u64", 5678uL)
        vd.setString("str", "test")
        vd.setByteArray("bytes", byteArrayOf(1, 2, 3))
        vd.setBool("bool", true)
        vd.setInt32("i32", -1)
        vd.setInt64("i64", -2L)

        val serialized = serialize(vd)
        val deserialized = deserialize<VariantDictionary>(serialized)

        assertEquals(1234u, deserialized.getUInt32("u32") as UInt)
        assertEquals(5678uL, deserialized.getUInt64("u64") as ULong)
        assertEquals("test", deserialized.getString("str"))
        assertArrayEquals(byteArrayOf(1, 2, 3), deserialized.getByteArray("bytes"))
        assertEquals(true, deserialized.getBool("bool"))
        assertEquals(-1, deserialized.getInt32("i32"))
        assertEquals(-2L, deserialized.getInt64("i64"))
    }

    @Test
    fun testAesKdfSerialization() {
        val aesKdf = AesKdf()
        aesKdf.setKeyRounds(123456uL)
        
        val serialized = serialize(aesKdf)
        val deserialized = deserialize<AesKdf>(serialized)
        
        assertEquals(aesKdf.uuid, deserialized.uuid)
        assertEquals(123456uL, deserialized.getKeyRounds())
    }

    @Test
    fun testArgon2KdfSerialization() {
        val argon2Kdf = Argon2Kdf(Argon2Kdf.Type.ARGON2_ID)
        argon2Kdf.setKeyRounds(10uL)
        argon2Kdf.setMemoryUsage(16384uL)
        argon2Kdf.setParallelism(2L)
        
        val serialized = serialize(argon2Kdf)
        val deserialized = deserialize<Argon2Kdf>(serialized)
        
        assertEquals(argon2Kdf.uuid, deserialized.uuid)
        assertEquals(10uL, deserialized.getKeyRounds())
        assertEquals(16384uL, deserialized.getMemoryUsage())
        assertEquals(2L, deserialized.getParallelism())
    }

    private fun serialize(obj: Any): ByteArray {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(obj)
        oos.close()
        return bos.toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserialize(bytes: ByteArray): T {
        val bis = ByteArrayInputStream(bytes)
        val ois = ObjectInputStream(bis)
        return ois.readObject() as T
    }
}
