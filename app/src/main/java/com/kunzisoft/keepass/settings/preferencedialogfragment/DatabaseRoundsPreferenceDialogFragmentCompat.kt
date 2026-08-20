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
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.kdf.KdfBenchmark
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_BENCHMARK_KDF
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.BenchmarkKdfRunnable
import com.kunzisoft.keepass.tasks.BenchmarkKdfRunnable.Companion.DEFAULT_BENCHMARK_TIME
import com.kunzisoft.keepass.utils.getParcelableCompat

class DatabaseRoundsPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private var calculateRounds: String? = null

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        explanationText = getString(R.string.rounds_explanation)
        setExplanationButton(getString(
            R.string.benchmark_calculation,
            "%.1f".format(DEFAULT_BENCHMARK_TIME / 1000.0)
        )) {
            mDatabaseViewModel.benchmarkKdf()
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        inputText = calculateRounds ?: database.numberKeyEncryptionRounds.toString()
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        if (actionTask == ACTION_DATABASE_BENCHMARK_KDF) {
            result.data?.getParcelableCompat<KdfBenchmark>(BenchmarkKdfRunnable.EXTRA_NEW_BENCHMARK)?.let { newBenchmark ->
                val stringRound = newBenchmark.iterations.toString()
                calculateRounds = stringRound
                inputText = stringRound
            }
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.kdfEngine?.let { kdfEngine ->
                val minIterations = kdfEngine.minKeyRounds
                var newRounds: ULong = try {
                    inputText.toLong().toULong()
                } catch (_: NumberFormatException) {
                    minIterations
                }
                if (newRounds < minIterations) {
                    newRounds = minIterations
                }
                val maxIterations = kdfEngine.maxKeyRounds
                if (newRounds > maxIterations) {
                    newRounds = maxIterations
                    Toast.makeText(
                        context,
                        getString(R.string.error_rounds_too_large,
                            maxIterations.toString()),
                        Toast.LENGTH_LONG
                    ).show()
                }

                val oldRounds = database.numberKeyEncryptionRounds
                database.numberKeyEncryptionRounds = newRounds

                saveIterations(oldRounds.toLong(), newRounds.toLong())
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseRoundsPreferenceDialogFragmentCompat {
            val fragment = DatabaseRoundsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
