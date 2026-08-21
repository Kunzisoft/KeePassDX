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
import com.kunzisoft.keepass.utils.AppUtil
import com.kunzisoft.keepass.utils.getParcelableCompat

class DatabaseParallelismPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.parallelism_explanation)
        val safeParallelismLimit = AppUtil.getSafeParallelismLimit()
        setExplanationButton(getString(R.string.parallelism_set_safe)) {
            inputText = safeParallelismLimit.toString()
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        inputText = database.parallelism.toString()
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        if (actionTask == ACTION_DATABASE_BENCHMARK_KDF) {
            result.data?.getParcelableCompat<KdfBenchmark>(BenchmarkKdfRunnable.EXTRA_NEW_BENCHMARK)?.let { newBenchmark ->
                inputText = newBenchmark.parallelism.toString()
            }
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.let {
                val minParallelism = database.kdfEngine?.minParallelism ?: DEFAULT_MIN_PARALLELISM
                var parallelism: Long = try {
                    inputText.toLong()
                } catch (_: NumberFormatException) {
                    minParallelism
                }
                if (parallelism < minParallelism) {
                    parallelism = minParallelism
                }
                val maxParallelism = database.kdfEngine?.maxParallelism ?: DEFAULT_MAX_PARALLELISM
                if (parallelism > maxParallelism) {
                    parallelism = maxParallelism
                    Toast.makeText(context, getString(R.string.error_parallelism_too_large, maxParallelism.toString()), Toast.LENGTH_LONG).show()
                }

                val oldParallelism = database.parallelism
                database.parallelism = parallelism

                saveParallelism(oldParallelism, parallelism)
            }
        }
    }

    companion object {

        private const val DEFAULT_MIN_PARALLELISM = 1L
        private const val DEFAULT_MAX_PARALLELISM = Long.MAX_VALUE

        fun newInstance(key: String): DatabaseParallelismPreferenceDialogFragmentCompat {
            val fragment = DatabaseParallelismPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
