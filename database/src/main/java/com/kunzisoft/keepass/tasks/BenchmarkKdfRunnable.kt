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
import com.kunzisoft.keepass.database.crypto.kdf.KdfBenchmark
import com.kunzisoft.keepass.database.crypto.kdf.KdfLimits
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.utils.getParcelableCompat

open class BenchmarkKdfRunnable(
    private val database: Database,
    private val limits: KdfLimits
): ActionRunnable() {

    override fun onStartRun() {}

    override fun onActionRun() {
        val engine = database.kdfEngine
            ?: throw IllegalStateException("No KDF engine found")
        val masterKey = database.masterKey

        val benchmark = engine.calculateBenchmark(
            masterKey = masterKey,
            targetTime = DEFAULT_BENCHMARK_TIME,
            limits = limits
        )
        result.data = Bundle().putNewBenchmark(benchmark)
    }

    override fun onFinishRun() {}

    companion object {
        const val EXTRA_NEW_BENCHMARK = "EXTRA_NEW_BENCHMARK"
        const val DEFAULT_BENCHMARK_TIME = 1000L


        /**
         * Put new benchmark associated with ACTION_DATABASE_BENCHMARK_KDF
         */
        fun Bundle.putNewBenchmark(benchmark: KdfBenchmark): Bundle {
            return this.apply { putParcelable(EXTRA_NEW_BENCHMARK, benchmark) }
        }

        /**
         * Retrieve new benchmark associated with ACTION_DATABASE_BENCHMARK_KDF
         */
        fun Bundle.retrieveNewBenchmark(): KdfBenchmark? {
            return getParcelableCompat<KdfBenchmark>(EXTRA_NEW_BENCHMARK)
        }
    }
}
