/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings.preferencedialogfragment

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.tasks.ActionRunnable

class MemoryUsagePreferenceDialogFragmentCompat : InputDatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.memory_usage_explanation)
        inputText = database?.memoryUsage?.toString()?: "0"
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            database?.let { database ->
                var memoryUsage: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    1
                }
                if (memoryUsage < 1) {
                    memoryUsage = 1
                }

                val oldMemoryUsage = database.memoryUsage
                database.memoryUsage = memoryUsage

                actionInUIThreadAfterSaveDatabase = AfterMemorySave(memoryUsage, oldMemoryUsage)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterMemorySave(private val mNewMemory: Long,
                                        private val mOldMemory: Long)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val memoryToShow = mNewMemory
            if (!result.isSuccess) {
                database?.memoryUsage = mOldMemory
            }
            preference.summary = memoryToShow.toString()
        }
    }

    companion object {

        fun newInstance(key: String): MemoryUsagePreferenceDialogFragmentCompat {
            val fragment = MemoryUsagePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
