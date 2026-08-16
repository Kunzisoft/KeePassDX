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
import com.kunzisoft.keepass.utils.DataByte

class DatabaseMemoryUsagePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    private var dataByte = DataByte(1L, DataByte.ByteFormat.BYTE)

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.memory_usage_explanation)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        dataByte = DataByte(
            database.memoryUsage.toLong(),
            DataByte.ByteFormat.BYTE
        ).toBetterByteFormat()
        inputText = dataByte.number.toString()
        setUnitText(dataByte.format.stringId)
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
                val oldMemoryUsage = database.memoryUsage
                database.memoryUsage = newMemoryUsage

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
