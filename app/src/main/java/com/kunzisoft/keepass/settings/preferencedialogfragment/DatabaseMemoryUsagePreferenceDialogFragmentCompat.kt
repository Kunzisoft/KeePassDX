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
import com.kunzisoft.keepass.utils.DataByte
import com.kunzisoft.keepass.utils.getParcelableCompat

class DatabaseMemoryUsagePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private var dataByte = DataByte(1L, DataByte.ByteFormat.BYTE)

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.memory_usage_explanation)
    }

    private fun setMemoryBytes(bytes: Long) {
        dataByte = DataByte(bytes, DataByte.ByteFormat.BYTE).toBetterByteFormat()
        inputText = dataByte.number.toString()
        setUnitText(dataByte.format.stringId)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        database.kdfEngine?.getMemoryUsage()?.toLong()?.let {
            setMemoryBytes(it)
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        if (actionTask == ACTION_DATABASE_BENCHMARK_KDF) {
            result.data?.getParcelableCompat<KdfBenchmark>(BenchmarkKdfRunnable.EXTRA_NEW_BENCHMARK)?.let { newBenchmark ->
                setMemoryBytes(newBenchmark.memory.toLong())
            }
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.kdfEngine?.let { kdfEngine ->
                val minMemoryUsage = kdfEngine.minMemoryUsage
                var newMemoryUsage: ULong = try {
                    // To transform in bytes
                    DataByte(inputText.toLong(), dataByte.format).toBytes().toULong()
                } catch (_: NumberFormatException) {
                    minMemoryUsage
                }
                if (newMemoryUsage < minMemoryUsage) {
                    newMemoryUsage = minMemoryUsage
                }
                val maxMemoryUsage = kdfEngine.maxMemoryUsage
                dataByte = DataByte(
                    newMemoryUsage.toLong(),
                    DataByte.ByteFormat.BYTE
                ).toBetterByteFormat()
                if (newMemoryUsage > maxMemoryUsage) {
                    newMemoryUsage = maxMemoryUsage
                    Toast.makeText(
                        context,
                        getString(
                            R.string.error_memory_too_large,
                            dataByte.toString(requireContext())),
                        Toast.LENGTH_LONG
                    ).show()
                }
                val oldMemoryUsage = kdfEngine.getMemoryUsage()
                kdfEngine.setMemoryUsage(newMemoryUsage)

                saveMemoryUsage(oldMemoryUsage.toLong(), newMemoryUsage.toLong())
            }
        }
    }

    companion object {

        fun newInstance(key: String): DatabaseMemoryUsagePreferenceDialogFragmentCompat {
            val fragment = DatabaseMemoryUsagePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
