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
package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.kunzisoft.androidclearchroma.ChromaPreferenceCompat

import com.kunzisoft.keepass.R

class DialogColorPreference @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                      defStyleRes: Int = defStyleAttr)
    : ChromaPreferenceCompat(context, attrs, defStyleAttr, defStyleRes) {

    override fun setSummary(summary: CharSequence?) {
        if (color == DISABLE_COLOR)
            super.setSummary("")
        else
            super.setSummary(summary)
    }

    override fun getDialogLayoutResource(): Int {
        return R.layout.pref_dialog_input_color
    }

    companion object {

        @ColorInt
        const val DISABLE_COLOR: Int = Color.TRANSPARENT
    }
}
