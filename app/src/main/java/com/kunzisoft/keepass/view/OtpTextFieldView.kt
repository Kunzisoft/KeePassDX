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
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil

/**
 * Custom text field view for displaying and managing OTP (One-Time Password) fields.
 * Extends [TextFieldView] to include an [OtpProgressView] for time-based tokens.
 */
open class OtpTextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextFieldView(context, attrs, defStyle) {

    private var mShowOTP: Boolean = PreferencesUtil.showOTPToken(context)

    private var otpProgressViewId = ViewCompat.generateViewId()

    private val otpProgressView = OtpProgressView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    init {
        buildViews()
        containerView.addView(otpProgressView)
    }

    /**
     * Sets the listener to be notified when the OTP token is updated.
     * @param onOtpUpdated The callback function.
     */
    fun setOnOtpUpdatedListener(onOtpUpdated: ((OtpElement?) -> Unit)? = null) {
        otpProgressView.onOtpUpdated = onOtpUpdated
    }

    /**
     * Initializes the views and layout rules for the OTP progress indicator.
     */
    private fun buildViews() {
        otpProgressView.apply {
            id = otpProgressViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(END_OF, labelViewId)
                it.addRule(RIGHT_OF, labelViewId)
            }
        }
    }

    /**
     * Sets the OTP model to be displayed and configures the protection settings.
     * @param otpModel The OTP model containing the secret and configuration.
     */
    fun setOtpModel(otpModel: OtpModel) {
        clearData()
        val otpElement = OtpElement(otpModel)
        setProtection(
            isProtected = !mShowOTP,
            isRevealedByDefault = isRevealed(),
            needUserVerificationToReveal = false
        )
        if (otpElement.token.isEmpty()) {
            otpProgressView.isVisible = false
            setLabel(R.string.entry_otp)
            setValue(R.string.error_invalid_OTP)
        } else {
            label = otpElement.type.name
            value = otpElement.tokenFormatted
            otpProgressView.apply {
                setOtpElement(otpElement)
                isVisible = true
            }
            textDirection = TEXT_DIRECTION_LTR
        }
    }

    /**
     * Clears all OTP data and stops progress updates.
     */
    fun clearData() {
        otpProgressView.clearData()
    }
}
