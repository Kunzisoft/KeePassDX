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

import java.io.IOException
import java.io.Serializable
import java.util.UUID
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

    @Transient
    var onParametersChanged: (() -> Unit)? = null

    /**
     * Checks the device's memory limits and throws a SecurityException if they are exceeded
     * @param limits Limits that must not be exceeded.
     */
    open fun checkLimits(limits: Limits) {}

    /**
     * Transform the key using the internal parameters and local limits.
     * @param masterKey The master key to transform.
     * @return The transformed key.
     */
    @Throws(IOException::class)
    abstract fun transform(masterKey: ByteArray): ByteArray

    /**
     * Measure the time to transform the key and return the optimized parameters to reach the target time.
     * @param masterKey The master key to transform.
     * @param targetTime The target time in milliseconds.
     * @param limits The local resource limits to apply during the benchmark.
     * @return The benchmark result containing optimized parameters.
     */
    fun calculateBenchmark(
        masterKey: ByteArray,
        targetTime: Long = DEFAULT_BENCHMARK_TIME,
        limits: KdfLimits
    ): KdfBenchmark {
        val currentRounds = getKeyRounds()
        val testRounds = if (currentRounds > maxKeyRounds) maxKeyRounds else currentRounds

        val savedParallelism = getParallelism()
        val savedMemory = getMemoryUsage()
        // Keep the exact number of processors
        setParallelism(limits.parallelism
            .coerceIn(minParallelism, maxParallelism))
        // Set the default memory or limit memory if it's below
        setMemoryUsage(min(limits.memory, defaultMemoryUsage)
            .coerceIn(minMemoryUsage, maxMemoryUsage))

        val time = measureTimeMillis {
            transform(masterKey)
        }
        val resultRounds = if (time > 0) {
            val newRounds = (testRounds.toDouble() * targetTime / time).toULong()
            newRounds.coerceIn(minKeyRounds, maxKeyRounds)
        } else {
            testRounds
        }

        // Restore old values
        val newParallelism = getParallelism()
        val newMemory = getMemoryUsage()
        setParallelism(savedParallelism)
        setMemoryUsage(savedMemory)

        return KdfBenchmark(
            iterations = resultRounds,
            memory = newMemory,
            parallelism = newParallelism
        )
    }

    /**
     * Measure the time to transform the key and assign the optimized value.
     * @param masterKey The master key to transform.
     * @param targetTime The target time in milliseconds.
     * @param limits The local resource limits to apply.
     */
    fun optimizeByBenchmark(
        masterKey: ByteArray,
        targetTime: Long = DEFAULT_BENCHMARK_TIME,
        limits: KdfLimits
    ) {
       calculateBenchmark(masterKey, targetTime, limits).also { result ->
           result.iterations?.let { setKeyRounds(it) }
           result.parallelism?.let { setParallelism(it) }
           result.memory?.let { setMemoryUsage(it) }
        }
    }

    /**
     * Define if the current parameters are the default ones.
     * @return True if parameters are default, false otherwise.
     */
    fun isDefault(): Boolean {
        return getKeyRounds() == defaultKeyRounds &&
                getMemoryUsage() == defaultMemoryUsage &&
                getParallelism() == defaultParallelism
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

    abstract fun getKeyRounds(): ULong

    abstract fun setKeyRounds(keyRounds: ULong)

    abstract val defaultKeyRounds: ULong

    open val minKeyRounds: ULong
        get() = 1u

    open val maxKeyRounds: ULong
        get() = ULong.MAX_VALUE

    /*
     * MEMORY
     */

    open fun getMemoryUsage(): ULong {
        return UNKNOWN_ULONG_VALUE
    }

    open fun setMemoryUsage(memory: ULong) {
        // Do nothing by default
    }

    open val defaultMemoryUsage: ULong
        get() = UNKNOWN_ULONG_VALUE

    open val minMemoryUsage: ULong
        get() = 1u

    open val maxMemoryUsage: ULong
        get() = ULong.MAX_VALUE

    /*
     * PARALLELISM
     */

    open fun getParallelism(): Long {
        return UNKNOWN_LONG_VALUE
    }

    open fun setParallelism(parallelism: Long) {
        // Do nothing by default
    }

    open val defaultParallelism: Long
        get() = UNKNOWN_LONG_VALUE

    open val minParallelism: Long
        get() = 1L

    open val maxParallelism: Long
        get() = UInt.MAX_VALUE.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KdfEngine) return false
        return uuid != null && uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid?.hashCode() ?: 0
    }

    companion object {
        const val UNKNOWN_LONG_VALUE: Long = 0L
        const val UNKNOWN_ULONG_VALUE: ULong = 0u

        const val DEFAULT_BENCHMARK_TIME = 1000L
    }
}
