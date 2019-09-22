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

class MaxHistorySizePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.max_history_size_summary)
        database?.historyMaxSize?.let { maxItemsDatabase ->
            inputText = maxItemsDatabase.toString()
            setSwitchAction({ isChecked ->
                inputText = if (!isChecked) {
                    INFINITE_MAX_HISTORY_SIZE.toString()
                } else
                    DEFAULT_MAX_HISTORY_SIZE.toString()
                showInputText(isChecked)
            }, maxItemsDatabase > INFINITE_MAX_HISTORY_SIZE)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            database?.let { database ->
                var maxHistorySize: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    DEFAULT_MAX_HISTORY_SIZE
                }
                if (maxHistorySize < INFINITE_MAX_HISTORY_SIZE) {
                    maxHistorySize = INFINITE_MAX_HISTORY_SIZE
                }

                val oldMaxHistorySize = database.historyMaxSize
                database.historyMaxSize = maxHistorySize

                actionInUIThreadAfterSaveDatabase = AfterMaxHistorySizeSave(maxHistorySize, oldMaxHistorySize)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterMaxHistorySizeSave(private val mNewMaxHistorySize: Long,
                                                private val mOldMaxHistorySize: Long)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val maxHistorySizeToShow =
                    if (result.isSuccess) {
                        mNewMaxHistorySize
                    } else {
                        database?.historyMaxSize = mOldMaxHistorySize
                        mOldMaxHistorySize
                    }
            preference.summary = maxHistorySizeToShow.toString()
        }
    }

    companion object {

        const val DEFAULT_MAX_HISTORY_SIZE = 134217728L
        const val INFINITE_MAX_HISTORY_SIZE = -1L

        fun newInstance(key: String): MaxHistorySizePreferenceDialogFragmentCompat {
            val fragment = MaxHistorySizePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
