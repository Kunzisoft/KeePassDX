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

import android.util.Log
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import com.kunzisoft.keepass.database.exception.KDFIterationDatabaseException
import com.kunzisoft.keepass.database.exception.KDFMemoryDatabaseException
import com.kunzisoft.keepass.database.exception.KDFParallelismDatabaseException
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

    override val defaultKeyRounds: ULong
        get() = DEFAULT_ITERATIONS

    @Throws(SecurityException::class)
    override fun checkLimits(limits: Limits) {
        val validated = getValidatedParameters()
        val parallelism = validated.parallelism.toLong()
        if (parallelism > limits.parallelism) {
            Log.w(TAG, "Parallelism is above the maximum number of processors.")
        }
        if (!limits.isMemorySufficient(validated.memory, Limits.LimitOperationType.KDF))
            throw KDFMemoryDatabaseException()
    }

    @Throws(IOException::class)
    override fun transform(masterKey: ByteArray): ByteArray {

        val salt = parameters.getByteArray(PARAM_SALT) ?: ByteArray(0)
        val validated = getValidatedParameters()

        val parallelism = validated.parallelism
        val memory = (validated.memory / MEMORY_BLOCK_SIZE).toUInt()
        val iterations = validated.iterations.toUInt()

        val version = parameters.getUInt32(PARAM_VERSION)?.toInt()
            ?.coerceIn(MIN_VERSION.toInt(), MAX_VERSION.toInt())
            ?: MAX_VERSION.toInt()
        if (version < MIN_VERSION.toInt()
            || version > MAX_VERSION.toInt())
            throw SecurityException("Version not in valid range")

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

    private data class ValidatedParams(
        val parallelism: UInt,
        val memory: ULong,
        val iterations: ULong,
    )

    private fun getValidatedParameters(): ValidatedParams {
        val parallelism = parameters.getUInt32(PARAM_PARALLELISM) ?: DEFAULT_PARALLELISM
        if (parallelism !in MIN_PARALLELISM..MAX_PARALLELISM)
            throw KDFParallelismDatabaseException()

        val memory = parameters.getUInt64(PARAM_MEMORY) ?: DEFAULT_MEMORY
        if (memory !in MIN_MEMORY..MAX_MEMORY)
            throw KDFMemoryDatabaseException()

        val iterations = parameters.getUInt64(PARAM_ITERATIONS) ?: DEFAULT_ITERATIONS
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS)
            throw KDFIterationDatabaseException()

        return ValidatedParams(parallelism, memory, iterations)
    }

    override fun randomize() {
        super.randomize()
        parameters.setByteArray(PARAM_SALT, HashManager.generateRandom(32))
    }

    override fun getSeed(): ByteArray? {
        return parameters.getByteArray(PARAM_SALT)
    }

    override fun getKeyRounds(): ULong {
        return (parameters.getUInt64(PARAM_ITERATIONS) ?: defaultKeyRounds)
            .coerceIn(minKeyRounds, maxKeyRounds)
    }

    override fun setKeyRounds(keyRounds: ULong) {
        parameters.setUInt64(
            PARAM_ITERATIONS,
            keyRounds.coerceIn(minKeyRounds, maxKeyRounds)
        )
        onParametersChanged?.invoke()
    }

    override val minKeyRounds: ULong = MIN_ITERATIONS

    override val maxKeyRounds: ULong = MAX_ITERATIONS

    override fun getMemoryUsage(): ULong {
        return (parameters.getUInt64(PARAM_MEMORY) ?: defaultMemoryUsage)
            .coerceIn(minMemoryUsage, maxMemoryUsage)
    }

    override fun setMemoryUsage(memory: ULong) {
        parameters.setUInt64(
            PARAM_MEMORY,
            memory.coerceIn(minMemoryUsage, maxMemoryUsage)
        )
        onParametersChanged?.invoke()
    }

    override val defaultMemoryUsage: ULong = DEFAULT_MEMORY

    override val minMemoryUsage: ULong = MIN_MEMORY

    override val maxMemoryUsage: ULong = MAX_MEMORY

    override fun getParallelism(): Long {
        return (parameters.getUInt32(PARAM_PARALLELISM)?.toLong() ?: defaultParallelism)
            .coerceIn(minParallelism, maxParallelism)
    }

    override fun setParallelism(parallelism: Long) {
        parameters.setUInt32(
            PARAM_PARALLELISM,
            parallelism.coerceIn(minParallelism, maxParallelism).toUInt()
        )
        onParametersChanged?.invoke()
    }

    override fun toString(): String {
        return "$type"
    }

    override val defaultParallelism: Long
        get() = DEFAULT_PARALLELISM.toLong()

    override val minParallelism: Long
        get() = MIN_PARALLELISM.toLong()

    override val maxParallelism: Long
        get() = MAX_PARALLELISM.toLong()

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
        private val TAG = Argon2Kdf::class.simpleName

        private const val PARAM_SALT = "S" // byte[]
        private const val PARAM_PARALLELISM = "P" // UInt32
        private const val PARAM_MEMORY = "M" // UInt64
        private const val PARAM_ITERATIONS = "I" // UInt64
        private const val PARAM_VERSION = "V" // UInt32
        private const val PARAM_SECRET_KEY = "K" // byte[]
        private const val PARAM_ASSOC_DATA = "A" // byte[]

        private const val MIN_VERSION: UInt = 0x10u
        private const val MAX_VERSION: UInt = 0x13u

        private val DEFAULT_ITERATIONS: ULong = 3u
        private val MIN_ITERATIONS: ULong = 1u
        private val MAX_ITERATIONS: ULong = UInt.MAX_VALUE.toULong() // Do not exceed the maximum UInt value

        private val DEFAULT_MEMORY: ULong = (MEMORY_BLOCK_SIZE * 1024u * 16u).toULong() // 16 MiB
        private val MIN_MEMORY: ULong = (MEMORY_BLOCK_SIZE * 8u).toULong() // 8 MiB
        private val MAX_MEMORY: ULong = UInt.MAX_VALUE.toULong()  // 4 GiB
        private const val MEMORY_BLOCK_SIZE: UInt = 1024u // to pass arguments to JNI

        private const val DEFAULT_PARALLELISM: UInt = 4u
        private const val MIN_PARALLELISM: UInt = 1u
        private val MAX_PARALLELISM: UInt = ((1 shl 24) - 1).toUInt()
    }
}
