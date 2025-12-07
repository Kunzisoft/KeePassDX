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
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper

class OtpDisplayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ProtectedFieldView {

    private val otpToken: TextView

    private var otpElement: OtpElement? = null
    private val otpProgress: CircularProgressIndicator
    private var mPrefSizeMultiplier: Float = PreferencesUtil.getListTextSize(context)
    private var mClipboardHelper: ClipboardHelper = ClipboardHelper(context)
    private var otpRunnable: OtpRunnable = OtpRunnable(this)

    private var mShowOTP: Boolean = PreferencesUtil.showOTPToken(context)

    private var mProtected = false
    private var mCurrentlyProtected = true
    private var mOnUnprotectClickListener: OnClickListener? = null

    class OtpRunnable(val view: OtpDisplayView?): Runnable {
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
        otpToken = findViewById(R.id.otp_token)
        otpProgress = findViewById(R.id.otp_progress)
    }

    fun setOtpElement(otpElement: OtpElement?) {
        this.removeCallbacks(otpRunnable)
        this.otpElement = otpElement
        if (otpElement != null
            && mShowOTP
            && otpElement.token.isNotEmpty()) {

            // Execute runnable to show progress
            otpRunnable.action = {
                populateOtpView()
            }
            if (otpElement.type == OtpType.TOTP) {
                otpRunnable.postDelayed()
            }
            populateOtpView()
            visibility = VISIBLE
        } else {
            visibility = GONE
        }

        setOnClickListener {
            otpElement?.token?.let { token ->
                try {
                    if (mProtected) {
                        unprotect()
                        mOnUnprotectClickListener?.onClick(this)
                    }
                    mClipboardHelper.copyToClipboard(
                        TemplateField.getLocalizedName(
                            context,
                            TemplateField.LABEL_TOKEN
                        ),
                        token,
                        true
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to copy the OTP token", e)
                }
            }
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
            otpToken.apply {
                val token = otpElement.tokenString
                if (mProtected && token != text) {
                    mCurrentlyProtected = true
                    applyHiddenStyle(isCurrentlyProtected())
                }
                text = token
                setTextSize(TypedValue.COMPLEX_UNIT_PX, otpToken.textSize, mPrefSizeMultiplier)
                textDirection = TEXT_DIRECTION_LTR
            }
        }
    }
    
    fun setTokenTextColor(color: Int) {
        otpToken.setTextColor(color)
    }
    
    fun setProgressIndicatorColor(color: Int) {
        otpProgress.setIndicatorColor(color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(otpRunnable)
    }

    override fun setProtection(
        protection: Boolean,
        isCurrentlyProtected: Boolean,
        onUnprotectClickListener: OnClickListener?
    ) {
        this.mProtected = protection
        this.mCurrentlyProtected = isCurrentlyProtected
        this.mOnUnprotectClickListener = onUnprotectClickListener
    }

    override fun isCurrentlyProtected(): Boolean {
        return mCurrentlyProtected
    }

    override fun protect() {
        mCurrentlyProtected = true
        populateOtpView()
    }

    override fun unprotect() {
        mCurrentlyProtected = false
        populateOtpView()
    }

    companion object {
        private val TAG = OtpDisplayView::class.java.name
    }
}
