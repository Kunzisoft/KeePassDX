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

open class BenchmarkKdfRunnable(
    private val database: Database
): ActionRunnable() {

    override fun onStartRun() {}

    override fun onActionRun() {
        val engine = database.kdfEngine
            ?: throw IllegalStateException("No KDF engine found")
        val masterKey = database.masterKey

        val newRounds = engine.benchmark(masterKey = masterKey, targetTime = DEFAULT_BENCHMARK_TIME)
        result.data = Bundle().apply {
            putLong(EXTRA_NEW_ROUNDS, newRounds)
        }
    }

    override fun onFinishRun() {}

    companion object {
        const val EXTRA_NEW_ROUNDS = "EXTRA_NEW_ROUNDS"
        const val DEFAULT_BENCHMARK_TIME = 1000L
    }
}
