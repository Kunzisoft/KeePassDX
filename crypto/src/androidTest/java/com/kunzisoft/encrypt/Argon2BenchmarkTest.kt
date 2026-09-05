package com.kunzisoft.encrypt

import android.os.Build
import android.util.Log
import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import org.junit.Test

class Argon2BenchmarkTest {

    private val password = "password".toByteArray()
    private val salt = "saltsaltsaltsalt".toByteArray()
    private val version = 0x13

    private fun run(
        type: Argon2Type,
        iterations: UInt,
        memory: UInt,
        parallelism: UInt
    ): Long {
        val start = System.nanoTime()
        Argon2Transformer.transformKey(
            type = type,
            password = password,
            salt = salt,
            parallelism = parallelism,
            memory = memory,
            iterations = iterations,
            version = version
        )
        return (System.nanoTime() - start) / 1_000_000
    }

    private fun benchmark(
        type: Argon2Type,
        iterations: UInt = 3u,
        memory: UInt = 1024u * 64u, // 64 MB
        parallelism: UInt = 4u,
        warmups: Int = 4,
        runs: Int = 12
    ) {
        repeat(warmups) { run(type, iterations, memory, parallelism) }

        val timesMs = LongArray(runs) { run(type, iterations, memory, parallelism) }
        val sorted = timesMs.sorted()
        val median = sorted[runs / 2]
        val avg = timesMs.average()
        val min = timesMs.min()
        val max = timesMs.max()

        // Cost is proportional to iterations * memory, so normalizing by it makes
        // runs with different parameters directly comparable.
        val mibPasses = iterations.toDouble() * memory.toDouble() / 1024.0
        val mibPassesPerSec = mibPasses / (median / 1000.0)

        Log.i(TAG, "abi=${Build.SUPPORTED_ABIS.firstOrNull()} device=${Build.MODEL}")
        Log.i(TAG, "type=$type iter=$iterations memory=${memory}KB parallelism=$parallelism")
        // Chronological order, not sorted: a run that drifts upward over time
        // means something else is changing (clocks, placement), and sorting the
        // list destroys exactly that evidence.
        Log.i(TAG, "runs(ms) in order=${timesMs.joinToString()}")
        Log.i(TAG, "sorted=${sorted.joinToString()} median=$median avg=$avg min=$min max=$max")
        Log.i(TAG, "cost=${mibPasses}MiB-passes throughput=${"%.0f".format(mibPassesPerSec)}MiB-passes/s")
    }

    @Test
    fun benchmarkArgon2i() {
        benchmark(Argon2Type.ARGON2_I)
    }

    @Test
    fun benchmarkArgon2d() {
        benchmark(Argon2Type.ARGON2_D)
    }

    @Test
    fun benchmarkArgon2id() {
        benchmark(Argon2Type.ARGON2_ID)
    }

    @Test
    fun benchmarkArgon2dVaultParameters() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 128u,
            memory = 1024u * 32u, // 32 MiB
            parallelism = 4u,
            warmups = 1,
            runs = 3
        )
    }

    /**
     * Same cost, reshaped towards memory instead of iterations. Should land close
     * to the run above; if it does not, the bottleneck is memory bandwidth rather
     * than the compression function.
     */
    @Test
    fun benchmarkArgon2dMemoryHeavy() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 1u,
            memory = 1024u * 512u, // 512 MiB
            parallelism = 4u,
            warmups = 1,
            runs = 3
        )
    }

    /**
     * Same total cost as [benchmarkArgon2dT2M512] but with the cost put into
     * iterations instead of memory, which halves the area-time product.
     */
    @Test
    fun benchmarkArgon2dT4M256() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 4u,
            memory = 1024u * 256u, // 256 MiB
            parallelism = 4u,
            warmups = 1,
            runs = 3
        )
    }

    @Test
    fun benchmarkArgon2dT2M512() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 2u,
            memory = 1024u * 512u, // 512 MiB
            parallelism = 4u,
            warmups = 1,
            runs = 3
        )
    }

    /**
     * On an asymmetric CPU the lanes join at every sync point, so the slowest
     * core gates the whole derivation. If fewer lanes are faster than more, the
     * little cores are the bottleneck rather than the compression function.
     */
    @Test
    fun benchmarkArgon2dTwoLanes() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 128u,
            memory = 1024u * 32u,
            parallelism = 2u,
            warmups = 0,
            runs = 2
        )
    }

    @Test
    fun benchmarkArgon2dSixLanes() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 128u,
            memory = 1024u * 32u,
            parallelism = 6u,
            warmups = 0,
            runs = 2
        )
    }

    /**
     * Single lane, to check whether the four Argon2 worker threads are actually
     * getting four cores. Should be close to 4x the p=4 run; much less than that
     * means the workers are being scheduled onto little cores.
     */
    @Test
    fun benchmarkArgon2dSingleLane() {
        benchmark(
            Argon2Type.ARGON2_D,
            iterations = 128u,
            memory = 1024u * 32u,
            parallelism = 1u,
            warmups = 0,
            runs = 2
        )
    }

    companion object {
        private const val TAG = "Argon2Benchmark"
    }
}
