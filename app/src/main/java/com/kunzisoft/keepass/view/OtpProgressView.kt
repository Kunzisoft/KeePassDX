package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType

class OtpProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val otpCircularProgress: CircularProgressIndicator
    private val otpTextProgress: TextView
    private var otpRunnable: OtpRunnable = OtpRunnable(this)

    var onOtpUpdated: ((OtpElement?) -> Unit)? = null

    private class OtpRunnable(val view: OtpProgressView?): Runnable {
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
        LayoutInflater.from(context).inflate(R.layout.view_otp_progress, this, true)
        otpCircularProgress = findViewById(R.id.otp_circular_progress)
        otpTextProgress = findViewById(R.id.otp_text_progress)
    }

    fun setOtpElement(otpElement: OtpElement?) {
        removeCallbacks(otpRunnable)
        otpRunnable = OtpRunnable(this)
        otpElement?.let { otpElement ->
            if (otpElement.token.isNotEmpty()) {
                // Execute runnable to show progress
                otpRunnable.action = {
                    this@OtpProgressView.onOtpUpdated?.invoke(otpElement)
                    populateOtpView(otpElement)
                }
                this@OtpProgressView.onOtpUpdated?.invoke(otpElement)
                if (otpElement.type == OtpType.TOTP) {
                    otpRunnable.postDelayed()
                }
                populateOtpView(otpElement)
                visibility = VISIBLE
            } else {
                visibility = GONE
            }
        } ?: run { visibility = GONE }
    }

    private fun populateOtpView(otpElement: OtpElement) {
        when (otpElement.type) {
            OtpType.HOTP -> {
                otpTextProgress.visibility = GONE
                otpCircularProgress.apply {
                    max = 100
                    setProgressCompat(100, true)
                }
            }
            OtpType.TOTP -> {
                val seconds = otpElement.secondsRemaining
                otpTextProgress.apply {
                    visibility = VISIBLE
                    text = seconds.toString()
                }
                otpCircularProgress.apply {
                    max = otpElement.period
                    setProgressCompat(seconds, true)
                }
            }
        }
    }
    
    fun setProgressColor(color: Int) {
        otpTextProgress.setTextColor(color)
        otpCircularProgress.setIndicatorColor(color)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(otpRunnable)
        clearData()
    }

    fun clearData() {
        removeCallbacks(otpRunnable)
        otpRunnable = OtpRunnable(this)
        visibility = GONE
    }

    companion object {
        private val TAG = OtpProgressView::class.java.name
    }
}
