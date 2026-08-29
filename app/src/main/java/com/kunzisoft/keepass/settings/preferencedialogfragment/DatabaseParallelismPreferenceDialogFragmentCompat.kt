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
import android.util.Log
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.kdf.KdfBenchmark
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_BENCHMARK_KDF
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getParcelableElements
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.AppUtil

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
        database.kdfEngine?.getParallelism()?.toString()?.let {
            inputText = it
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        if (actionTask == ACTION_DATABASE_BENCHMARK_KDF) {
            result.data?.getParcelableElements<KdfBenchmark> { _, newBenchmark ->
                newBenchmark?.parallelism?.let { parallelism ->
                    inputText = parallelism.toString()
                }
            }
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.kdfEngine?.let { kdfEngine ->
                val minParallelism = kdfEngine.minParallelism
                var parallelism: Long = try {
                    inputText.toLong()
                } catch (_: NumberFormatException) {
                    minParallelism
                }
                if (parallelism < minParallelism) {
                    parallelism = minParallelism
                }
                val maxParallelism = kdfEngine.maxParallelism
                if (parallelism > maxParallelism) {
                    parallelism = maxParallelism
                    Log.e(TAG, getString(
                            R.string.error_parallelism_too_large,
                            maxParallelism.toString()))
                }

                val oldParallelism = kdfEngine.getParallelism()
                kdfEngine.setParallelism(parallelism)

                saveParallelism(oldParallelism, parallelism)
            }
        }
    }

    companion object {
        private val TAG = DatabaseParallelismPreferenceDialogFragmentCompat::class.simpleName

        fun newInstance(key: String): DatabaseParallelismPreferenceDialogFragmentCompat {
            val fragment = DatabaseParallelismPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
