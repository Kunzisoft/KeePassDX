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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseMaxHistoryItemsPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        setExplanationText(R.string.max_history_items_summary)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        database?.historyMaxItems?.let { maxItemsDatabase ->
            inputText = maxItemsDatabase.toString()
            setSwitchAction({ isChecked ->
                inputText = if (!isChecked) {
                    NONE_MAX_HISTORY_ITEMS.toString()
                } else {
                    DEFAULT_MAX_HISTORY_ITEMS.toString()
                }
                showInputText(isChecked)
            }, maxItemsDatabase > NONE_MAX_HISTORY_ITEMS)
        }
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                var maxHistoryItems: Int = try {
                    inputText.toInt()
                } catch (e: NumberFormatException) {
                    DEFAULT_MAX_HISTORY_ITEMS
                }
                if (maxHistoryItems < NONE_MAX_HISTORY_ITEMS) {
                    maxHistoryItems = NONE_MAX_HISTORY_ITEMS
                }

                val oldMaxHistoryItems = database.historyMaxItems
                database.historyMaxItems = maxHistoryItems

                // Remove all history items
                database.removeOldestHistoryForEachEntry()

                saveMaxHistoryItems(oldMaxHistoryItems, maxHistoryItems)
            }
        }
    }

    companion object {

        const val DEFAULT_MAX_HISTORY_ITEMS = 10
        const val NONE_MAX_HISTORY_ITEMS = -1

        fun newInstance(key: String): DatabaseMaxHistoryItemsPreferenceDialogFragmentCompat {
            val fragment = DatabaseMaxHistoryItemsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
