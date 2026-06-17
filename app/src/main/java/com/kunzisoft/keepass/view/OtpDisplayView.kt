/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper

/**
 * View to display an OTP token and its progress.
 * It supports protection (masking) and copying to clipboard.
 */
class OtpDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ProtectedFieldView {

    private val otpProgress: OtpProgressView
    private val otpTokenView: TextView

    private var mPrefSizeMultiplier: Float = PreferencesUtil.getListTextSize(context)
    private var mClipboardHelper: ClipboardHelper = ClipboardHelper(context)

    private var mShowOTP: Boolean = PreferencesUtil.showOTPToken(context)

    private var otpElement: OtpElement? = null
    private var mProtected = true
    private var mRevealed = mShowOTP
    private var mNeedUserVerificationToReveal = false

    override var onRevealChanged: ((isRevealed: Boolean) -> Unit)? = null

    /**
     * Callback when the OTP is updated.
     */
    var onOtpUpdated: ((OtpElement?) -> Unit)? = null


    init {
        LayoutInflater.from(context).inflate(R.layout.view_otp_display, this, true)
        otpProgress = findViewById(R.id.otp_progress)
        otpTokenView = findViewById(R.id.otp_token)
    }

    /**
     * Copy the current OTP token to the clipboard.
     */
    fun copyTokenToClipboard() {
        otpElement?.token?.let { token ->
            try {
                mClipboardHelper.copyToClipboard(
                    TemplateField.getLocalizedName(context, TemplateField.LABEL_TOKEN),
                    token,
                    true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to copy the OTP token", e)
            }
        }
    }

    /**
     * Hide the token view (invisible).
     */
    fun hideToken() {
        otpTokenView.visibility = INVISIBLE
    }

    /**
     * Reveal the token if it's protected.
     */
    fun revealToken() {
        if (mProtected) {
            reveal()
            populateOtpToken()
        }
    }

    /**
     * Mask the token if it's protected.
     */
    fun maskToken() {
        if (mProtected) {
            mask()
            populateOtpToken()
        }
    }

    /**
     * Set the OTP model and update the view.
     * @param otpModel The OTP model to display.
     */
    fun setOtpModel(otpModel: OtpModel?) {
        mRevealed = mShowOTP
        otpModel?.let { model ->
            val otpElement = OtpElement(model)
            this.otpElement = otpElement
            if (otpElement.token.isNotEmpty()) {
                otpProgress.apply {
                    this.onOtpUpdated = {
                        if (otpTokenView.text != String(otpElement.tokenFormatted)) {
                            if (!mShowOTP) {
                                mask()
                            }
                        }
                        this@OtpDisplayView.onOtpUpdated?.invoke(otpElement)
                        populateOtpToken()
                    }
                    setOtpElement(otpElement)
                }
                visibility = VISIBLE
            } else {
                visibility = GONE
            }
        } ?: run { visibility = GONE }

        setOnClickListener {
            copyTokenToClipboard()
        }

        setOnLongClickListener {
            revealToken()
            true
        }
    }

    /**
     * Update the token view with the current OTP element.
     */
    private fun populateOtpToken() {
        otpElement?.let { otpElement ->
            otpTokenView.apply {
                if (if (mProtected) mRevealed else true) {
                    showByFading()
                } else {
                    visibility = GONE
                }
                text = String(otpElement.tokenFormatted)
                setTextSize(
                    unit = TypedValue.COMPLEX_UNIT_PX,
                    defaultSize = otpTokenView.textSize,
                    mPrefSizeMultiplier
                )
                textDirection = TEXT_DIRECTION_LTR
            }
        }
    }
    
    /**
     * Set the text color of the OTP token.
     * @param color The color to set.
     */
    fun setTokenTextColor(color: Int) {
        otpTokenView.setTextColor(color)
    }
    
    /**
     * Set the progress indicator color.
     * @param color The color to set.
     */
    fun setProgressIndicatorColor(color: Int) {
        otpProgress.setProgressColor(color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearData()
    }

    override fun setProtection(
        isProtected: Boolean,
        isRevealedByDefault: Boolean,
        needUserVerificationToReveal: Boolean
    ) {
        this.mProtected = isProtected
        this.mRevealed = isRevealedByDefault
        this.mNeedUserVerificationToReveal = needUserVerificationToReveal
    }

    override fun isRevealed(): Boolean {
        return mRevealed
    }

    override fun mask() {
        if (mRevealed) {
            mRevealed = false
            onRevealChanged?.invoke(false)
        }
    }

    override fun reveal() {
        if (!mRevealed) {
            mRevealed = true
            onRevealChanged?.invoke(true)
        }
    }

    /**
     * Clear the data and stop any pending updates.
     */
    fun clearData() {
        otpElement = null
        otpProgress.clearData()
        visibility = GONE
    }

    companion object {
        private val TAG = OtpDisplayView::class.java.name
    }
}
