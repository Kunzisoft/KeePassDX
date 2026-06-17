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

    var onOtpUpdated: ((OtpElement?) -> Unit)? = null


    init {
        LayoutInflater.from(context).inflate(R.layout.view_otp_display, this, true)
        otpProgress = findViewById(R.id.otp_progress)
        otpTokenView = findViewById(R.id.otp_token)
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
            populateOtpToken()
        }
    }

    fun maskToken() {
        if (mProtected) {
            mask()
            populateOtpToken()
        }
    }

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
                    defaultSize = otpTokenView.
                    textSize,
                    mPrefSizeMultiplier
                )
                textDirection = TEXT_DIRECTION_LTR
            }
        }
    }
    
    fun setTokenTextColor(color: Int) {
        otpTokenView.setTextColor(color)
    }
    
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

    fun clearData() {
        otpElement = null
        otpProgress.clearData()
        visibility = GONE
    }

    companion object {
        private val TAG = OtpDisplayView::class.java.name
    }
}
