/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordEntropy


class PasskeyTextFieldView @JvmOverloads constructor(context: Context,
                                                     attrs: AttributeSet? = null,
                                                     defStyle: Int = 0)
    : PasswordTextFieldView(context, attrs, defStyle) {

    private var relyingPartyViewId = ViewCompat.generateViewId()
    private var usernameViewId = ViewCompat.generateViewId()
    private var passkeyImageId = ViewCompat.generateViewId()

    private var passkeyImage = AppCompatImageView(
        ContextThemeWrapper(context, R.style.KeepassDXStyle_ImageButton_Simple), null, 0).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT)
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_passkey_white_24dp))
        contentDescription = context.getString(R.string.passkey)
    }

    private val relyingPartyView = AppCompatTextView(context).apply {
        setTextAppearance(context,
            R.style.KeepassDXStyle_TextAppearance_TextNodePrimary)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.marginStart = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
        }
        setTextIsSelectable(true)
    }
    private val usernameView = AppCompatTextView(context).apply {
        setTextAppearance(context,
            R.style.KeepassDXStyle_TextAppearance_TextNodeSecondary)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT).also {
            it.topMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4f,
                resources.displayMetrics
            ).toInt()
            it.leftMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
            it.marginStart = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()
        }
        setTextIsSelectable(true)
    }

    private fun buildViews() {
        indicatorDrawable?.let {
            DrawableCompat.setTint(it, PasswordEntropy.Strength.VERY_UNGUESSABLE.color)
        }
        passkeyImage.apply {
            id = passkeyImageId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(ALIGN_PARENT_RIGHT)
                it.addRule(ALIGN_PARENT_END)
            }
        }
        labelView.apply {
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, passkeyImageId)
                it.addRule(START_OF, passkeyImageId)
            }
        }
        relyingPartyView.apply {
            id = relyingPartyViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, passkeyImageId)
                it.addRule(START_OF, passkeyImageId)
                it.addRule(BELOW, labelViewId)
            }
        }
        usernameView.apply {
            id = usernameViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, passkeyImageId)
                it.addRule(START_OF, passkeyImageId)
                it.addRule(BELOW, relyingPartyViewId)
            }
        }
    }

    init {
        removeAllViews()
        buildViews()
        addView(passkeyImage)
        addView(labelView)
        addView(relyingPartyView)
        addView(usernameView)
    }

    override var default: String = ""
    override var isFieldVisible: Boolean = true

    var relyingParty: String
        get() {
            return relyingPartyView.text.toString()
        }
        set(value) {
            relyingPartyView.text = value
        }

    var username: String
        get() {
            return usernameView.text.toString()
        }
        set(value) {
            usernameView.text = value
        }

    override fun getEntropyStrength(passwordText: String) {
        // Do nothing
    }
}
