/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.password.PasswordEntropy
import com.kunzisoft.keepass.settings.PreferencesUtil

class PassKeyView @JvmOverloads constructor(context: Context,
                                            attrs: AttributeSet? = null,
                                            defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var mPasswordEntropyCalculator: PasswordEntropy? = null

    private val passwordInputLayout: TextInputLayout
    private val passwordText: TextView
    private val passwordStrengthProgress: LinearProgressIndicator
    private val passwordEntropy: TextView

    private var mViewHint: String = ""
    private var mMaxLines: Int = 3
    private var mShowPassword: Boolean = false

    private var mPasswordTextWatcher: MutableList<TextWatcher> = mutableListOf()
    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            mPasswordTextWatcher.forEach {
                it.beforeTextChanged(charSequence, i, i1, i2)
            }
        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            mPasswordTextWatcher.forEach {
                it.onTextChanged(charSequence, i, i1, i2)
            }
        }

        override fun afterTextChanged(editable: Editable) {
            mPasswordTextWatcher.forEach {
                it.afterTextChanged(editable)
            }
            getEntropyStrength(editable.toString())
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PassKeyView,
            0, 0).apply {
            try {
                mViewHint = getString(R.styleable.PassKeyView_passKeyHint)
                    ?: context.getString(R.string.password)
                mMaxLines = getInteger(R.styleable.PassKeyView_passKeyMaxLines, mMaxLines)
                mShowPassword = getBoolean(R.styleable.PassKeyView_passKeyVisible,
                    !PreferencesUtil.hideProtectedValue(context))
            } finally {
                recycle()
            }
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_passkey, this)

        passwordInputLayout = findViewById(R.id.password_input_layout)
        passwordInputLayout?.hint = mViewHint
        passwordText = findViewById(R.id.password_text)
        if (mShowPassword) {
            passwordText?.inputType = passwordText.inputType or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        passwordText?.maxLines = mMaxLines
        passwordText?.applyFontVisibility()
        passwordText.addTextChangedListener(passwordTextWatcher)
        passwordStrengthProgress = findViewById(R.id.password_strength_progress)
        passwordStrengthProgress?.apply {
            setIndicatorColor(PasswordEntropy.Strength.RISKY.color)
            progress = 0
            max = 100
        }
        passwordEntropy = findViewById(R.id.password_entropy)

        mPasswordEntropyCalculator = PasswordEntropy {
            passwordText?.text?.toString()?.let { firstPassword ->
                getEntropyStrength(firstPassword)
            }
        }
    }

    private fun getEntropyStrength(passwordText: String) {
        mPasswordEntropyCalculator?.getEntropyStrength(passwordText) { entropyStrength ->
            passwordStrengthProgress.apply {
                post {
                    setIndicatorColor(entropyStrength.strength.color)
                    setProgressCompat(entropyStrength.estimationPercent, true)
                }
            }
            passwordEntropy.apply {
                post {
                    text = PasswordEntropy.getStringEntropy(resources, entropyStrength.entropy)
                }
            }
        }
    }

    fun addTextChangedListener(textWatcher: TextWatcher) {
        mPasswordTextWatcher.add(textWatcher)
    }

    fun removeTextChangedListener(textWatcher: TextWatcher) {
        mPasswordTextWatcher.remove(textWatcher)
    }

    var passwordString: String
        get() {
            return passwordText.text.toString()
        }
        set(value) {
            val spannableString =
                if (PreferencesUtil.colorizePassword(context))
                    PasswordGenerator.getColorizedPassword(value)
                else
                    SpannableString(value)
            passwordText.text = spannableString
        }
}