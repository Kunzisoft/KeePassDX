package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper

class OtpDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ProtectedFieldView {

    private val otpTokenView: TextView

    private var otpElement: OtpElement? = null
    private val otpProgress: CircularProgressIndicator
    private var mPrefSizeMultiplier: Float = PreferencesUtil.getListTextSize(context)
    private var mClipboardHelper: ClipboardHelper = ClipboardHelper(context)
    private var otpRunnable: OtpRunnable = OtpRunnable(this)

    private var mFontInVisibility: Boolean = PreferencesUtil.fieldFontIsInVisibility(context)
    private var mShowOTP: Boolean = PreferencesUtil.showOTPToken(context)

    private var mProtected = true
    private var mRevealed = mShowOTP
    private var mNeedUserVerificationToReveal = false

    override var onRevealChanged: ((isRevealed: Boolean) -> Unit)? = null

    var onOtpUpdated: ((OtpElement?) -> Unit)? = null

    private class OtpRunnable(val view: OtpDisplayView?): Runnable {
        var action: (() -> Unit)? = null
        override fun run() {
            action?.invoke()
            postDelayed()
        }
        fun postDelayed() {
            view?.postDelayed(this, 1000)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_otp_display, this, true)
        otpProgress = findViewById(R.id.otp_progress)
        otpTokenView = findViewById(R.id.otp_token)
        if (mFontInVisibility)
            otpTokenView.applyFontVisibility()
    }

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

    fun hideToken() {
        otpTokenView.visibility = INVISIBLE
    }

    fun revealToken() {
        if (mProtected) {
            reveal()
            populateOtpView()
        }
    }

    fun maskToken() {
        if (mProtected) {
            mask()
            populateOtpView()
        }
    }

    fun setOtpModel(otpModel: OtpModel?) {
        mRevealed = mShowOTP
        removeCallbacks(otpRunnable)
        otpRunnable = OtpRunnable(this)
        otpModel?.let { model ->
            val otpElement = OtpElement(model)
            this.otpElement = otpElement
            if (otpElement.token.isNotEmpty()) {
                // Execute runnable to show progress
                otpRunnable.action = {
                    if (otpTokenView.text != String(otpElement.tokenFormatted)) {
                        if (!mShowOTP) {
                            mask()
                        }
                    }
                    this@OtpDisplayView.onOtpUpdated?.invoke(otpElement)
                    populateOtpView()
                }
                this@OtpDisplayView.onOtpUpdated?.invoke(otpElement)
                if (otpElement.type == OtpType.TOTP) {
                    otpRunnable.postDelayed()
                }
                populateOtpView()
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

    private fun populateOtpView() {
        otpElement?.let { otpElement ->
            when (otpElement.type) {
                OtpType.HOTP -> {
                    otpProgress.apply {
                        max = 100
                        setProgressCompat(100, true)
                    }
                }
                OtpType.TOTP -> {
                    otpProgress.apply {
                        max = otpElement.period
                        setProgressCompat(otpElement.secondsRemaining, true)
                    }
                }
            }
            otpTokenView.apply {
                applyHiddenStyle(if (mProtected) !mRevealed else false)
                text = String(otpElement.tokenFormatted)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, otpTokenView.textSize, mPrefSizeMultiplier)
                textDirection = TEXT_DIRECTION_LTR
            }
        }
    }
    
    fun setTokenTextColor(color: Int) {
        otpTokenView.setTextColor(color)
    }
    
    fun setProgressIndicatorColor(color: Int) {
        otpProgress.setIndicatorColor(color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(otpRunnable)
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

    fun clearData() {
        otpElement = null
        removeCallbacks(otpRunnable)
        otpRunnable = OtpRunnable(this)
        visibility = GONE
    }

    companion object {
        private val TAG = OtpDisplayView::class.java.name
    }
}
