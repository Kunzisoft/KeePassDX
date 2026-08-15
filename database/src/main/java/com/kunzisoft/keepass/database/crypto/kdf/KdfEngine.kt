/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.utils.UnsignedInt
import java.io.IOException
import java.io.Serializable
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

abstract class KdfEngine : Serializable {

    var uuid: UUID? = null

    abstract val defaultParameters: KdfParameters

    var parameters: KdfParameters = KdfParameters(UUID(0, 0))
        get() {
            if (field.uuid == UUID(0, 0)) {
                field = defaultParameters
            }
            return field
        }

    var isParametersRandomized = false

    /**
     * Transform the key using the internal parameters.
     * @param masterKey The master key to transform.
     * @return The transformed key.
     */
    @Throws(IOException::class)
    abstract fun transform(masterKey: ByteArray): ByteArray

    /**
     * Measure the time to transform the key and return the number of rounds to reach the target time.
     * @param masterKey The master key to transform.
     * @param targetTime The target time in milliseconds.
     * @return The number of rounds to reach the target time.
     */
    fun benchmark(
        masterKey: ByteArray,
        targetTime: Long = 1000L
    ): Long {
        val currentRounds = getKeyRounds()
        val testRounds = if (currentRounds > 0) currentRounds else defaultKeyRounds
        val time = measureTimeMillis {
            transform(masterKey)
        }
        return if (time > 0) {
            val newRounds = (testRounds.toDouble() * targetTime / time).toLong()
            max(minKeyRounds, min(maxKeyRounds, newRounds))
        } else {
            testRounds
        }
    }

    /**
     * Measure the time to transform the key and assign the optimized value
     * @param masterKey The master key to transform.
     * @param targetTime The target time in milliseconds.
     */
    fun optimizeByBenchmark(
        masterKey: ByteArray,
        targetTime: Long = 1000L
    ) {
        setKeyRounds(benchmark(masterKey, targetTime))
    }

    /**
     * Randomize the internal parameters.
     */
    open fun randomize() {
        isParametersRandomized = true
    }

    /**
     * Get the seed or salt from the KDF parameters.
     * @return The seed or salt.
     */
    open fun getSeed(): ByteArray? = null

    /*
     * ITERATIONS
     */

    abstract fun getKeyRounds(): Long

    abstract fun setKeyRounds(keyRounds: Long)

    abstract val defaultKeyRounds: Long

    open val minKeyRounds: Long
        get() = 1

    open val maxKeyRounds: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    /*
     * MEMORY
     */

    open fun getMemoryUsage(): Long {
        return UNKNOWN_VALUE
    }

    open fun setMemoryUsage(memory: Long) {
        // Do nothing by default
    }

    open val defaultMemoryUsage: Long
        get() = UNKNOWN_VALUE

    open val minMemoryUsage: Long
        get() = 1

    open val maxMemoryUsage: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    /*
     * PARALLELISM
     */

    open fun getParallelism(): Long {
        return UNKNOWN_VALUE
    }

    open fun setParallelism(parallelism: Long) {
        // Do nothing by default
    }

    open val defaultParallelism: Long
        get() = UNKNOWN_VALUE

    open val minParallelism: Long
        get() = 1L

    open val maxParallelism: Long
        get() = UnsignedInt.MAX_VALUE.toKotlinLong()

    companion object {
        const val UNKNOWN_VALUE: Long = -1L
    }
}
