/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
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
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordEntropy
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil


class PasswordTextFieldView @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyle: Int = 0)
    : TextFieldView(context, attrs, defStyle) {

    private var mPasswordEntropyCalculator: PasswordEntropy = PasswordEntropy {
        valueView.text?.toString()?.let { firstPassword ->
            getEntropyStrength(firstPassword)
        }
    }

    private var indicatorDrawable = ContextCompat.getDrawable(
        context,
        R.drawable.ic_shield_white_24dp
    )?.apply {
        val lineHeight = labelView.lineHeight
        setBounds(0,0,lineHeight, lineHeight)
        DrawableCompat.setTint(this, Color.TRANSPARENT)
    }

    override var label: String
        get() {
            return labelView.text.toString().removeSuffix(ICON_STRING_SPACES)
        }
        set(value) {
            indicatorDrawable?.let { drawable ->
                val spannableString = SpannableString("$value$ICON_STRING_SPACES")
                val startPosition = spannableString.split(ICON_STRING)[0].length
                val endPosition = startPosition + ICON_STRING.length
                spannableString
                    .setSpan(
                        ImageSpan(drawable),
                        startPosition,
                        endPosition,
                        SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                labelView.text = spannableString
            } ?: kotlin.run {
                labelView.text = value
            }
        }

    override fun setLabel(@StringRes labelId: Int) {
        label = resources.getString(labelId)
    }

    override var value: String
        get() {
            return valueView.text.toString()
        }
        set(value) {
            val spannableString =
                if (PreferencesUtil.colorizePassword(context))
                    PasswordGenerator.getColorizedPassword(value)
                else
                    SpannableString(value)
            valueView.text = spannableString
            changeProtectedValueParameters()
        }

    override fun setValue(@StringRes valueId: Int) {
        value = resources.getString(valueId)
    }

    private fun getEntropyStrength(passwordText: String) {
        mPasswordEntropyCalculator.getEntropyStrength(passwordText) { entropyStrength ->
            labelView.apply {
                post {
                    val strengthColor = entropyStrength.strength.color
                    indicatorDrawable?.let { drawable ->
                        DrawableCompat.setTint(drawable, strengthColor)
                    }
                    invalidate()
                }
            }
        }
    }

    companion object {
        private const val ICON_STRING = "[icon]"
        private const val ICON_STRING_SPACES = "  $ICON_STRING"
    }
}
