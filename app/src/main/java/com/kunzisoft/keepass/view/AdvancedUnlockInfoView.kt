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
package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockInfoView @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var biometricButtonView: Button? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_advanced_unlock, this)

        biometricButtonView = findViewById(R.id.biometric_button)
    }

    fun setIconViewClickListener(listener: OnClickListener?) {
        biometricButtonView?.setOnClickListener(listener)
    }

    var title: CharSequence
        get() {
            return biometricButtonView?.text?.toString() ?: ""
        }
        set(value) {
            biometricButtonView?.text = value
        }

    fun setTitle(@StringRes textId: Int) {
        title = context.getString(textId)
    }

    fun setMessage(text: CharSequence) {
        if (text.isNotEmpty())
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun setMessage(@StringRes textId: Int) {
        Toast.makeText(context, textId, Toast.LENGTH_LONG).show()
    }

}