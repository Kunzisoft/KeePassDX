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

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.preference.PreferenceDialogFragmentCompat
import com.kunzisoft.keepass.R

abstract class InputPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {

    private var inputTextView: EditText? = null
    private var textUnitView: TextView? = null
    private var textExplanationView: TextView? = null
    private var explanationButton: Button? = null
    private var switchElementView: CompoundButton? = null

    private var mOnInputTextEditorActionListener: TextView.OnEditorActionListener? = null

    var inputText: String
        get() = this.inputTextView?.text?.toString() ?: ""
        set(inputText) {
            if (inputTextView != null) {
                this.inputTextView?.setText(inputText)
                this.inputTextView?.setSelection(this.inputTextView!!.text.length)
            }
        }

    fun setInoutText(@StringRes inputTextId: Int) {
        inputText = getString(inputTextId)
    }

    fun showInputText(show: Boolean) {
        inputTextView?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setInputTextError(error: CharSequence) {
        this.inputTextView?.error = error
    }

    fun setOnInputTextEditorActionListener(onEditorActionListener: TextView.OnEditorActionListener) {
        this.mOnInputTextEditorActionListener = onEditorActionListener
    }

    var unitText: String?
        get() = textUnitView?.text?.toString() ?: ""
        set(unitText) {
            textUnitView?.apply {
                if (!unitText.isNullOrEmpty()) {
                    text = unitText
                    visibility = View.VISIBLE
                } else {
                    text = ""
                    visibility = View.GONE
                }
            }
        }

    fun setUnitText(@StringRes unitTextId: Int) {
        unitText = getString(unitTextId)
    }

    var explanationText: String?
        get() = textExplanationView?.text?.toString() ?: ""
        set(explanationText) {
            textExplanationView?.apply {
                if (!explanationText.isNullOrEmpty()) {
                    text = explanationText
                    visibility = View.VISIBLE
                } else {
                    text = ""
                    visibility = View.GONE
                }
            }
        }

    fun setExplanationText(@StringRes explanationTextId: Int) {
        explanationText = getString(explanationTextId)
    }

    val explanationButtonText: String?
        get() = explanationButton?.text?.toString() ?: ""

    fun setExplanationButton(explanationButtonText: String?, clickListener: View.OnClickListener) {
        explanationButton?.apply {
            if (!explanationButtonText.isNullOrEmpty()) {
                text = explanationButtonText
                visibility = View.VISIBLE
                setOnClickListener(clickListener)
            } else {
                text = ""
                visibility = View.GONE
                setOnClickListener(null)
            }
        }
    }

    fun setExplanationButton(@StringRes explanationButtonTextId: Int, clickListener: View.OnClickListener) {
        setExplanationButton(getString(explanationButtonTextId), clickListener)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        inputTextView = view.findViewById(R.id.input_text)
        inputTextView?.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v, actionId, event ->
                if (mOnInputTextEditorActionListener == null) {
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
                } else {
                    mOnInputTextEditorActionListener?.onEditorAction(v, actionId, event)
                            ?: false
                }
            }
        }
        textUnitView = view.findViewById(R.id.input_text_unit)
        textUnitView?.visibility = View.GONE
        textExplanationView = view.findViewById(R.id.explanation_text)
        textExplanationView?.visibility = View.GONE
        explanationButton = view.findViewById(R.id.explanation_button)
        explanationButton?.visibility = View.GONE
        switchElementView = view.findViewById(R.id.switch_element)
        switchElementView?.visibility = View.GONE
    }

    fun setSwitchAction(onCheckedChange: ((isChecked: Boolean)-> Unit)?, defaultChecked: Boolean) {
        switchElementView?.visibility = if (onCheckedChange == null) View.GONE else View.VISIBLE
        switchElementView?.isChecked = defaultChecked
        inputTextView?.visibility = if (defaultChecked) View.VISIBLE else View.GONE
        switchElementView?.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange?.invoke(isChecked)
        }
    }

    fun isSwitchActivated(): Boolean {
        return switchElementView?.isChecked == true
    }

    fun activateSwitch() {
        if (switchElementView?.isChecked != true)
            switchElementView?.isChecked = true
    }

    fun deactivateSwitch() {
        if (switchElementView?.isChecked == true)
            switchElementView?.isChecked = false
    }
}
