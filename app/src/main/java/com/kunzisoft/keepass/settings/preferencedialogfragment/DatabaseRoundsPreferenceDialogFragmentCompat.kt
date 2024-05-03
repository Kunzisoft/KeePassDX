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

class DatabaseRoundsPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        explanationText = getString(R.string.rounds_explanation)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        inputText = database?.numberKeyEncryptionRounds?.toString() ?: MIN_ITERATIONS.toString()
    }

    override fun onDialogClosed(database: ContextualDatabase?, positiveResult: Boolean) {
        if (positiveResult) {
            database?.let {
                var rounds: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    MIN_ITERATIONS
                }
                if (rounds < MIN_ITERATIONS) {
                    rounds = MIN_ITERATIONS
                }
                // TODO Max iterations

                val oldRounds = database.numberKeyEncryptionRounds
                try {
                    database.numberKeyEncryptionRounds = rounds
                } catch (e: NumberFormatException) {
                    Toast.makeText(context, R.string.error_rounds_too_large, Toast.LENGTH_LONG).show()
                    database.numberKeyEncryptionRounds = Long.MAX_VALUE
                }

                saveIterations(oldRounds, rounds)
            }
        }
    }

    companion object {

        const val MIN_ITERATIONS = 1L

        fun newInstance(key: String): DatabaseRoundsPreferenceDialogFragmentCompat {
            val fragment = DatabaseRoundsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
