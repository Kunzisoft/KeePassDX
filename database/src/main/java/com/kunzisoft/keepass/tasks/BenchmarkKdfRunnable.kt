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
package com.kunzisoft.keepass.tasks

import android.os.Bundle
import com.kunzisoft.keepass.database.element.Database
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

open class BenchmarkKdfRunnable(
    private val database: Database
): ActionRunnable() {

    override fun onStartRun() {}

    override fun onActionRun() {
        val engine = database.kdfEngine ?: throw IllegalStateException("No KDF engine found")
        val parameters = database.kdfParameters ?: throw IllegalStateException("No KDF parameters found")
        val masterKey = database.masterKey

        val currentRounds = engine.getKeyRounds(parameters)
        
        // Use a small number of rounds if current rounds are too high for a quick benchmark
        val testRounds = if (currentRounds > 0) currentRounds else engine.defaultKeyRounds
        val time = measureTimeMillis {
            engine.transform(masterKey, parameters)
        }

        if (time > 0) {
            val targetTime = DEFAULT_BENCHMARK_TIME // TODO As parameter
            var newRounds = (testRounds.toDouble() * targetTime / time).toLong()
            newRounds = max(engine.minKeyRounds, min(engine.maxKeyRounds, newRounds))
            result.data = Bundle().apply {
                putLong(EXTRA_NEW_ROUNDS, newRounds)
            }
        } else {
            // If too fast, just fail (should not happen for KDF)
            setError("Benchmark failed: execution time too short")
        }
    }

    override fun onFinishRun() {}

    companion object {
        const val EXTRA_NEW_ROUNDS = "EXTRA_NEW_ROUNDS"
        const val DEFAULT_BENCHMARK_TIME = 1000L
    }
}
