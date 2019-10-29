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

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.preference.PreferenceDialogFragmentCompat
import com.kunzisoft.keepass.R

abstract class InputPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private var inputTextView: EditText? = null
    private var textExplanationView: TextView? = null
    private var switchElementView: CompoundButton? = null

    var inputText: String
        get() = this.inputTextView?.text?.toString() ?: ""
        set(inputText) {
            if (inputTextView != null) {
                this.inputTextView?.setText(inputText)
                this.inputTextView?.setSelection(this.inputTextView!!.text.length)
            }
        }

    var explanationText: String?
        get() = textExplanationView?.text?.toString() ?: ""
        set(explanationText) {
            textExplanationView?.apply {
                if (explanationText != null && explanationText.isNotEmpty()) {
                    text = explanationText
                    visibility = View.VISIBLE
                } else {
                    text = ""
                    visibility = View.GONE
                }
            }
        }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        inputTextView = view.findViewById(R.id.input_text)
        inputTextView?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        onDialogClosed(true)
                        dialog?.dismiss()
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        }
        textExplanationView = view.findViewById(R.id.explanation_text)
        textExplanationView?.visibility = View.GONE
        switchElementView = view.findViewById(R.id.switch_element)
        switchElementView?.visibility = View.GONE
    }

    fun setInoutText(@StringRes inputTextId: Int) {
        inputText = getString(inputTextId)
    }

    fun showInputText(show: Boolean) {
        inputTextView?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setExplanationText(@StringRes explanationTextId: Int) {
        explanationText = getString(explanationTextId)
    }

    fun setSwitchAction(onCheckedChange: ((isChecked: Boolean)-> Unit)?, defaultChecked: Boolean) {
        switchElementView?.visibility = if (onCheckedChange == null) View.GONE else View.VISIBLE
        switchElementView?.isChecked = defaultChecked
        inputTextView?.visibility = if (defaultChecked) View.VISIBLE else View.GONE
        switchElementView?.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange?.invoke(isChecked)
        }
    }
}
