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
import com.kunzisoft.keepass.tasks.ActionRunnable

class MaxHistoryItemsPreferenceDialogFragmentCompat : InputDatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        inputText = database?.historyMaxItems?.toString() ?: "0"
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            database?.let { database ->
                var maxHistoryItems: Int = try {
                    inputText.toInt()
                } catch (e: NumberFormatException) {
                    1
                }
                if (maxHistoryItems < 1) {
                    maxHistoryItems = 1
                }

                val oldMaxHistoryItems = database.historyMaxItems
                database.historyMaxItems = maxHistoryItems

                actionInUIThreadAfterSaveDatabase = AfterMaxHistoryItemsSave(maxHistoryItems, oldMaxHistoryItems)
            }
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterMaxHistoryItemsSave(private val mNewMaxHistoryItems: Int,
                                                 private val mOldMaxHistoryItems: Int)
        : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            var maxHistoryItemsToShow = mOldMaxHistoryItems
            if (!result.isSuccess) {
                maxHistoryItemsToShow = mNewMaxHistoryItems
                database?.historyMaxItems = mNewMaxHistoryItems
            }
            preference.summary = maxHistoryItemsToShow.toString()
        }
    }

    companion object {

        fun newInstance(key: String): MaxHistoryItemsPreferenceDialogFragmentCompat {
            val fragment = MaxHistoryItemsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
