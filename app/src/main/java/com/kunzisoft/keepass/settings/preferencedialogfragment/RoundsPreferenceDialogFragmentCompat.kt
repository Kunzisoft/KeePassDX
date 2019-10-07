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
import android.widget.Toast
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.tasks.ActionRunnable

class RoundsPreferenceDialogFragmentCompat : InputDatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        explanationText = getString(R.string.rounds_explanation)
        inputText = database?.numberKeyEncryptionRoundsAsString ?: ""
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (database != null && positiveResult) {
            var rounds: Long
            try {
                val strRounds = inputText
                rounds = java.lang.Long.parseLong(strRounds)
            } catch (e: NumberFormatException) {
                Toast.makeText(context, R.string.error_rounds_not_number, Toast.LENGTH_LONG).show()
                return
            }

            if (rounds < 1) {
                rounds = 1
            }

            val oldRounds = database!!.numberKeyEncryptionRounds
            try {
                database?.numberKeyEncryptionRounds = rounds
            } catch (e: NumberFormatException) {
                Toast.makeText(context, R.string.error_rounds_too_large, Toast.LENGTH_LONG).show()
                database?.numberKeyEncryptionRounds = Integer.MAX_VALUE.toLong()
            }

            actionInUIThreadAfterSaveDatabase = AfterRoundSave(rounds, oldRounds)
        }

        super.onDialogClosed(positiveResult)
    }

    private inner class AfterRoundSave(private val mNewRounds: Long,
                                       private val mOldRounds: Long) : ActionRunnable() {

        override fun onFinishRun(result: Result) {
            val roundsToShow = mNewRounds
            if (!result.isSuccess) {
                database?.numberKeyEncryptionRounds = mOldRounds
            }

            preference.summary = roundsToShow.toString()
        }
    }

    companion object {

        fun newInstance(key: String): RoundsPreferenceDialogFragmentCompat {
            val fragment = RoundsPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
