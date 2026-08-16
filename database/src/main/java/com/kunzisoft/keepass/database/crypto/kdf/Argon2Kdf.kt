/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.UnsignedLong
import com.kunzisoft.keepass.utils.bytes16ToUuid
import java.io.IOException
import java.util.UUID

class Argon2Kdf(private val type: Type) : KdfEngine() {

    init {
        uuid = type.CIPHER_UUID
    }

    override val defaultParameters: KdfParameters
        get() {
            val p = KdfParameters(uuid!!)

            p.setParamUUID()
            p.setUInt32(PARAM_PARALLELISM, DEFAULT_PARALLELISM)
            p.setUInt64(PARAM_MEMORY, DEFAULT_MEMORY)
            p.setUInt64(PARAM_ITERATIONS, DEFAULT_ITERATIONS)
            p.setUInt32(PARAM_VERSION, MAX_VERSION)

            return p
        }

    override val defaultKeyRounds: Long
        get() = DEFAULT_ITERATIONS.toKotlinLong()

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray): ByteArray {

        val salt = parameters.getByteArray(PARAM_SALT) ?: ByteArray(0)
        val parallelism = (parameters.getUInt32(PARAM_PARALLELISM)?.toKotlinLong() ?: DEFAULT_PARALLELISM.toKotlinLong())
            .coerceIn(minParallelism, maxParallelism)
        val memory = (parameters.getUInt64(PARAM_MEMORY)?.toKotlinLong()?.div(MEMORY_BLOCK_SIZE)
            ?: DEFAULT_MEMORY.toKotlinLong().div(MEMORY_BLOCK_SIZE))
            .coerceIn(minMemoryUsage / MEMORY_BLOCK_SIZE, maxMemoryUsage / MEMORY_BLOCK_SIZE)
        val iterations = (parameters.getUInt64(PARAM_ITERATIONS)?.toKotlinLong() ?: DEFAULT_ITERATIONS.toKotlinLong())
            .coerceIn(minKeyRounds, maxKeyRounds)
        val version = parameters.getUInt32(PARAM_VERSION)?.toKotlinInt() ?: MAX_VERSION.toKotlinInt()

        // Not used
        // val secretKey = parameters.getByteArray(PARAM_SECRET_KEY)
        // val assocData = parameters.getByteArray(PARAM_ASSOC_DATA)

        val argonType = if (type == Type.ARGON2_ID) Argon2Type.ARGON2_ID else Argon2Type.ARGON2_D

        return Argon2Transformer.transformKey(
                argonType,
                masterKey,
                salt,
                parallelism,
                memory,
                iterations,
                version
        )
    }

    override fun randomize() {
        super.randomize()
        parameters.setByteArray(PARAM_SALT, HashManager.generateRandom(32))
    }

    override fun getSeed(): ByteArray? {
        return parameters.getByteArray(PARAM_SALT)
    }

    override fun getKeyRounds(): Long {
        return parameters.getUInt64(PARAM_ITERATIONS)?.toKotlinLong() ?: defaultKeyRounds
    }

    override fun setKeyRounds(keyRounds: Long) {
        parameters.setUInt64(PARAM_ITERATIONS, UnsignedLong(keyRounds.coerceIn(minKeyRounds, maxKeyRounds)))
    }

    override val minKeyRounds: Long
        get() = MIN_ITERATIONS.toKotlinLong()

    override val maxKeyRounds: Long
        get() = MAX_ITERATIONS.toKotlinLong()

    override fun getMemoryUsage(): Long {
        return parameters.getUInt64(PARAM_MEMORY)?.toKotlinLong() ?: defaultMemoryUsage
    }

    override fun setMemoryUsage(memory: Long) {
        parameters.setUInt64(PARAM_MEMORY, UnsignedLong(memory.coerceIn(minMemoryUsage, maxMemoryUsage)))
    }

    override val defaultMemoryUsage: Long
        get() = DEFAULT_MEMORY.toKotlinLong()

    override val minMemoryUsage: Long
        get() = MIN_MEMORY.toKotlinLong()

    override val maxMemoryUsage: Long
        get() = MAX_MEMORY.toKotlinLong()

    override fun getParallelism(): Long {
        return parameters.getUInt32(PARAM_PARALLELISM)?.let {
            UnsignedInt(it).toKotlinLong()
        } ?: defaultParallelism
    }

    override fun setParallelism(parallelism: Long) {
        parameters.setUInt32(PARAM_PARALLELISM, UnsignedInt.fromKotlinLong(parallelism.coerceIn(minParallelism, maxParallelism)))
    }

    override fun toString(): String {
        return "$type"
    }

    override val defaultParallelism: Long
        get() = DEFAULT_PARALLELISM.toKotlinLong()

    override val minParallelism: Long
        get() = MIN_PARALLELISM.toKotlinLong()

    override val maxParallelism: Long
        get() = MAX_PARALLELISM.toKotlinLong()

    enum class Type(val CIPHER_UUID: UUID, private val typeName: String) {
        ARGON2_D(bytes16ToUuid(
                byteArrayOf(0xEF.toByte(),
                        0x63.toByte(),
                        0x6D.toByte(),
                        0xDF.toByte(),
                        0x8C.toByte(),
                        0x29.toByte(),
                        0x44.toByte(),
                        0x4B.toByte(),
                        0x91.toByte(),
                        0xF7.toByte(),
                        0xA9.toByte(),
                        0xA4.toByte(),
                        0x03.toByte(),
                        0xE3.toByte(),
                        0x0A.toByte(),
                        0x0C.toByte())), "Argon2d"),
        ARGON2_ID(bytes16ToUuid(
                byteArrayOf(0x9E.toByte(),
                        0x29.toByte(),
                        0x8B.toByte(),
                        0x19.toByte(),
                        0x56.toByte(),
                        0xDB.toByte(),
                        0x47.toByte(),
                        0x73.toByte(),
                        0xB2.toByte(),
                        0x3D.toByte(),
                        0xFC.toByte(),
                        0x3E.toByte(),
                        0xC6.toByte(),
                        0xF0.toByte(),
                        0xA1.toByte(),
                        0xE6.toByte())), "Argon2id");

        override fun toString(): String {
            return typeName
        }
    }

    companion object {

        private const val PARAM_SALT = "S" // byte[]
        private const val PARAM_PARALLELISM = "P" // UInt32
        private const val PARAM_MEMORY = "M" // UInt64
        private const val PARAM_ITERATIONS = "I" // UInt64
        private const val PARAM_VERSION = "V" // UInt32
        private const val PARAM_SECRET_KEY = "K" // byte[]
        private const val PARAM_ASSOC_DATA = "A" // byte[]

        private val MIN_VERSION = UnsignedInt(0x10)
        private val MAX_VERSION = UnsignedInt(0x13)

        private val DEFAULT_ITERATIONS = UnsignedLong(3L)
        private val MIN_ITERATIONS = UnsignedLong(1L)
        private val MAX_ITERATIONS = UnsignedLong(1_000_000L)

        private val DEFAULT_MEMORY = UnsignedLong((1024L * 1024L * 16L))
        private val MIN_MEMORY = UnsignedLong(1024L * 8L)
        private val MAX_MEMORY = UnsignedInt.MAX_VALUE
        private const val MEMORY_BLOCK_SIZE: Long = 1024L

        private val DEFAULT_PARALLELISM = UnsignedInt(4)
        private val MIN_PARALLELISM = UnsignedInt.fromKotlinLong(1L)
        private val MAX_PARALLELISM = UnsignedInt.fromKotlinLong(128L)
    }
}
