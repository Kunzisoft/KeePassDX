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
import com.kunzisoft.keepass.database.ContextualDatabase

class DatabaseNamePreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.name ?: ""
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        super.onDialogClosed(database, positiveResult)
        if (positiveResult) {
            database?.let {
                val newName = inputText
                val oldName = database.name
                database.name = newName
                saveName(oldName, newName)
            }
        }
    }

    companion object {

        fun newInstance(
                key: String): DatabaseNamePreferenceDialogFragmentCompat {
            val fragment = DatabaseNamePreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
