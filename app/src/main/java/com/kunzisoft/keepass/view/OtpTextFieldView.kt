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
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil


open class OtpTextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextFieldView(context, attrs, defStyle) {

    private var mShowOTP: Boolean = PreferencesUtil.showOTPToken(context)

    private var otpDisplayViewId = ViewCompat.generateViewId()

    private val otpDisplayView = OtpDisplayView(context).apply {
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
    }

    override var onRevealChanged: ((isRevealed: Boolean) -> Unit)? = null

    init {
        buildViews()
        addView(otpDisplayView)
    }

    fun setOnOtpUpdatedListener(onOtpUpdated: ((OtpElement?) -> Unit)? = null) {
        otpDisplayView.onOtpUpdated = onOtpUpdated
    }

    private fun buildViews() {
        otpDisplayView.apply {
            id = otpDisplayViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, showButtonId)
                it.addRule(START_OF, showButtonId)
                it.addRule(BELOW, labelViewId)
            }
        }
    }

    fun setOtpModel(otpModel: OtpModel) {
        clearData()
        val otpElement = OtpElement(otpModel)
        setProtection(
            isProtected = !mShowOTP,
            isRevealedByDefault = false,
            needUserVerificationToReveal = false
        )
        if (otpElement.token.isEmpty()) {
            otpDisplayView.isVisible = false
            setLabel(R.string.entry_otp)
            setValue(R.string.error_invalid_OTP)
        } else {
            label = otpElement.type.name
            value = otpElement.tokenFormatted
            otpDisplayView.apply {
                setOtpModel(otpModel)
                // Use the text field and not the token in display
                hideToken()
                isVisible = true
                setProtection(
                    isProtected = !mShowOTP,
                    isRevealedByDefault = isRevealed(),
                    needUserVerificationToReveal = false
                )
            }
            textDirection = TEXT_DIRECTION_LTR
        }
    }

    fun clearData() {
        otpDisplayView.clearData()
    }
}
