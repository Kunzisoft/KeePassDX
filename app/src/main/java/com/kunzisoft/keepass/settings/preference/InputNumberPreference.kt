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
package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine

class InputNumberPreference @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                      defStyleRes: Int = defStyleAttr)
    : InputTextExplanationPreference(context, attrs, defStyleAttr, defStyleRes) {

    // Save to Shared Preferences
    var number: Long = 0
        set(number) {
            field = number
            persistLong(number)
        }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_numbers
    }

    override fun setSummary(summary: CharSequence) {
        if (summary == KdfEngine.UNKNOWN_VALUE_STRING) {
            isEnabled = false
            super.setSummary("")
        } else {
            isEnabled = true
            super.setSummary(summary)
        }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        // Default value from attribute. Fallback value is set to 0.
        return a?.getInt(index, 0) ?: 0
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean,
                                   defaultValue: Any?) {
        // Read the value. Use the default value if it is not possible.
        var numberValue: Long
        if (!restorePersistedValue) {
            numberValue = 100000
            if (defaultValue is String) {
                numberValue = java.lang.Long.parseLong(defaultValue)
            }
            if (defaultValue is Int) {
                numberValue = defaultValue.toLong()
            }
            try {
                numberValue = defaultValue as Long
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            numberValue = getPersistedLong(this.number)
        }

        number = numberValue
    }

}
