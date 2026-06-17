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
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * View to display the progress of an OTP.
 */
class OtpProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val otpCircularProgress: CircularProgressIndicator
    private val otpTextProgress: TextView

    private var currentOtpElement: OtpElement? = null
    private var updateJob: Job? = null
    private var viewScope: CoroutineScope? = null

    /**
     * Callback when the OTP is updated.
     */
    var onOtpUpdated: ((OtpElement?) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_otp_progress, this, true)
        otpCircularProgress = findViewById(R.id.otp_circular_progress)
        otpTextProgress = findViewById(R.id.otp_text_progress)
    }

    private fun getScope(): CoroutineScope {
        return viewScope ?: CoroutineScope(Dispatchers.Main + SupervisorJob()).also { viewScope = it }
    }

    /**
     * Set the OTP element to display and start the progress update if it's a TOTP.
     *
     * @param otpElement The OTP element to display.
     */
    fun setOtpElement(otpElement: OtpElement?) {
        this.currentOtpElement = otpElement
        stopUpdateJob()
        if (otpElement != null && otpElement.token.isNotEmpty()) {
            visibility = VISIBLE
            onOtpUpdated?.invoke(otpElement)
            populateOtpView(otpElement)
            if (otpElement.type == OtpType.TOTP) {
                startUpdateJob()
            }
        } else {
            visibility = GONE
        }
    }

    private fun startUpdateJob() {
        val otpElement = currentOtpElement ?: return
        if (updateJob?.isActive == true) return

        updateJob = getScope().launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val delayTime = 1000 - (now % 1000)
                delay(delayTime)

                onOtpUpdated?.invoke(otpElement)
                populateOtpView(otpElement)
            }
        }
    }

    private fun stopUpdateJob() {
        updateJob?.cancel()
        updateJob = null
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

    /**
     * Set the color of the progress indicator and the text.
     *
     * @param color The color to set.
     */
    fun setProgressColor(color: Int) {
        otpTextProgress.setTextColor(color)
        otpCircularProgress.setIndicatorColor(color)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (currentOtpElement?.type == OtpType.TOTP) {
            startUpdateJob()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopUpdateJob()
        viewScope?.cancel()
        viewScope = null
    }

    /**
     * Clear the data and stop any pending updates.
     */
    fun clearData() {
        currentOtpElement = null
        stopUpdateJob()
        visibility = GONE
        onOtpUpdated = null
    }
}
