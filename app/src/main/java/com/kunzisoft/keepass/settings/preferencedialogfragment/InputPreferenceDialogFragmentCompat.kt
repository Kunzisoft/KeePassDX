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

import androidx.annotation.StringRes
import androidx.preference.PreferenceDialogFragmentCompat
import android.view.View
import android.widget.TextView

import com.kunzisoft.keepass.R

abstract class InputPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private var textExplanationView: TextView? = null

    var explanationText: String?
        get() = textExplanationView?.text?.toString() ?: ""
        set(explanationText) {
            if (explanationText != null && explanationText.isNotEmpty()) {
                textExplanationView?.text = explanationText
                textExplanationView?.visibility = View.VISIBLE
            } else {
                textExplanationView?.text = explanationText
                textExplanationView?.visibility = View.VISIBLE
            }
        }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        textExplanationView = view.findViewById(R.id.explanation_text)
    }

    fun setExplanationText(@StringRes explanationTextId: Int) {
        explanationText = getString(explanationTextId)
    }
}
