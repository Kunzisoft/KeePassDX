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
import android.text.Spannable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordEntropy
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil


class PasswordTextEditFieldView @JvmOverloads constructor(context: Context,
                                                          attrs: AttributeSet? = null,
                                                          defStyle: Int = 0)
    : TextEditFieldView(context, attrs, defStyle) {

    private var mPasswordEntropyCalculator: PasswordEntropy = PasswordEntropy {
        valueView.text?.toString()?.let { firstPassword ->
            getEntropyStrength(firstPassword)
        }
    }
    private var isColorizedPasswordActivated = PreferencesUtil.colorizePassword(context)

    private var passwordProgressViewId = ViewCompat.generateViewId()
    private var passwordEntropyViewId = ViewCompat.generateViewId()

    private var mPasswordProgress = LinearProgressIndicator(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(ALIGN_PARENT_BOTTOM)
        }
        setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                context.resources.displayMetrics
            ).toInt()
        )
        setIndicatorColor(PasswordEntropy.Strength.RISKY.color)
        progress = 0
        max = 100
    }

    private val mPasswordEntropyView = TextView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(ALIGN_PARENT_BOTTOM)
        }
        setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                context.resources.displayMetrics
            ).toInt()
        )
        TextViewCompat.setTextAppearance(this, R.style.KeepassDXStyle_Text_Indicator)
    }

    init {
        buildViews()

        valueView.doAfterTextChanged { editable ->
            getEntropyStrength(editable.toString())
            PasswordGenerator.colorizedPassword(editable)
        }

        addView(mPasswordProgress)
        addView(mPasswordEntropyView)
    }

    private fun buildViews() {
        mPasswordProgress.apply {
            id = passwordProgressViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, actionImageButtonId)
                it.addRule(START_OF, actionImageButtonId)
            }
        }
        mPasswordEntropyView.apply {
            id = passwordEntropyViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(ALIGN_RIGHT, passwordProgressViewId)
                it.addRule(ALIGN_END, passwordProgressViewId)
            }
        }
    }

    private fun getEntropyStrength(passwordText: String) {
        mPasswordEntropyCalculator.getEntropyStrength(passwordText) { entropyStrength ->
            mPasswordProgress.apply {
                post {
                    setIndicatorColor(entropyStrength.strength.color)
                    setProgressCompat(entropyStrength.estimationPercent, true)
                }
            }
            mPasswordEntropyView.apply {
                post {
                    text = PasswordEntropy.getStringEntropy(resources, entropyStrength.entropy)
                }
            }
        }
    }

    override fun spannableValue(value: String?): Spannable? {
        if (value == null)
            return null
        return if (isColorizedPasswordActivated)
            PasswordGenerator.getColorizedPassword(value)
        else
            super.spannableValue(value)
    }

    override var label: String
        get() {
            return super.label
        }
        set(value) {
            super.label = value
            // Define views Ids with label value
            passwordProgressViewId = "passwordProgressViewId $value".hashCode()
            passwordEntropyViewId = "passwordEntropyViewId $value".hashCode()
            buildViews()
        }
}