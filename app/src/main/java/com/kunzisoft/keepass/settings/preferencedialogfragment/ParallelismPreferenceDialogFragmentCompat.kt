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

class ParallelismPreferenceDialogFragmentCompat : DatabaseSavePreferenceDialogFragmentCompat() {

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        setExplanationText(R.string.parallelism_explanation)
        inputText = database?.parallelism?.toString() ?: MIN_PARALLELISM.toString()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            database?.let { database ->
                val parallelism: Long = try {
                    inputText.toLong()
                } catch (e: NumberFormatException) {
                    MIN_PARALLELISM
                }

                val oldParallelism = database.parallelism
                database.parallelism = parallelism

                mProgressDatabaseTaskProvider?.startDatabaseSaveParallelism(
                        oldParallelism,
                        parallelism,
                        mDatabaseAutoSaveEnable)
            }
        }
    }

    companion object {

        const val MIN_PARALLELISM = 1L

        fun newInstance(key: String): ParallelismPreferenceDialogFragmentCompat {
            val fragment = ParallelismPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle

            return fragment
        }
    }
}
