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
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordEntropy
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil

class PasswordEditView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var mPasswordEntropyCalculator: PasswordEntropy? = null

    private val passwordInputLayout: TextInputLayout
    private val passwordText: EditText
    private val passwordStrengthProgress: LinearProgressIndicator
    private val passwordEntropy: TextView

    private var mViewHint: String = ""
    private var mMaxLines: Int = 3
    private var mShowPassword: Boolean = false

    private var mPasswordTextWatchers: MutableList<TextWatcher> = mutableListOf()
    private var mPasswordTextWatcher: TextWatcher? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PasswordView,
            0, 0).apply {
            try {
                mViewHint = getString(R.styleable.PasswordView_passwordHint)
                    ?: context.getString(R.string.password)
                mMaxLines = getInteger(R.styleable.PasswordView_passwordMaxLines, mMaxLines)
                mShowPassword = getBoolean(R.styleable.PasswordView_passwordVisible,
                    !PreferencesUtil.hideProtectedValue(context))
            } finally {
                recycle()
            }
        }

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_password_edit, this)

        passwordInputLayout = findViewById(R.id.password_edit_input_layout)
        passwordInputLayout?.hint = mViewHint
        passwordText = findViewById(R.id.password_edit_text)
        if (mShowPassword) {
            passwordText?.inputType = passwordText.inputType or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        passwordText?.maxLines = mMaxLines
        passwordText?.applyFontVisibility()
        passwordStrengthProgress = findViewById(R.id.password_edit_strength_progress)
        passwordStrengthProgress?.apply {
            setIndicatorColor(PasswordEntropy.Strength.RISKY.color)
            progress = 0
            max = 100
        }
        passwordEntropy = findViewById(R.id.password_edit_entropy)

        mPasswordEntropyCalculator = PasswordEntropy {
            passwordText?.text?.toString()?.let { firstPassword ->
                getEntropyStrength(firstPassword)
            }
        }

        mPasswordTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mPasswordTextWatchers.forEach {
                    it.beforeTextChanged(charSequence, i, i1, i2)
                }
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                mPasswordTextWatchers.forEach {
                    it.onTextChanged(charSequence, i, i1, i2)
                }
            }

            override fun afterTextChanged(editable: Editable) {
                mPasswordTextWatchers.forEach {
                    it.afterTextChanged(editable)
                }
                getEntropyStrength(editable.toString())
                PasswordGenerator.colorizedPassword(editable)
            }
        }
        passwordText?.addTextChangedListener(mPasswordTextWatcher)
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
        mPasswordTextWatchers.add(textWatcher)
    }

    fun removeTextChangedListener(textWatcher: TextWatcher) {
        mPasswordTextWatchers.remove(textWatcher)
    }

    private fun spannableValue(value: String): Spannable {
        return if (PreferencesUtil.colorizePassword(context))
            PasswordGenerator.getColorizedPassword(value)
        else
            SpannableString(value)
    }

    var passwordString: String
        get() {
            return passwordText.text.toString()
        }
        set(value) {
            passwordText.setText(spannableValue(value))
        }
}