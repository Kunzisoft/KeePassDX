/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.biometric.FingerPrintAnimatedVector

@RequiresApi(api = Build.VERSION_CODES.M)
class AdvancedUnlockInfoView @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val unlockContainerView: View
    private var unlockAnimatedVector: FingerPrintAnimatedVector? = null
    private var unlockTitleTextView: TextView? = null
    private var unlockMessageTextView: TextView? = null
    var unlockIconImageView: ImageView? = null

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_advanced_unlock, this)

        unlockContainerView = findViewById(R.id.fingerprint_container)
        unlockTitleTextView = findViewById(R.id.biometric_title)
        unlockMessageTextView = findViewById(R.id.biometric_message)
        unlockIconImageView = findViewById(R.id.biometric_image)
    }

    private fun startIconViewAnimation() {
        unlockAnimatedVector?.startScan()
    }

    private fun stopIconViewAnimation() {
        unlockAnimatedVector?.stopScan()
    }

    fun setIconResource(iconId: Int) {
        unlockIconImageView?.setImageResource(iconId)
        // Init the fingerprint animation
        unlockAnimatedVector = when (iconId) {
            R.drawable.fingerprint -> FingerPrintAnimatedVector(context, unlockIconImageView!!)
            else -> null
        }
    }

    fun setIconViewClickListener(animation: Boolean = true,
                                 listener: ((view: View)->Unit)?) {
        var animateButton = animation
        if (listener == null)
            animateButton = false
        if (animateButton) {
            startIconViewAnimation()
            unlockContainerView.alpha = 1f
        } else {
            stopIconViewAnimation()
            unlockContainerView.alpha = 0.8f
        }
        unlockIconImageView?.setOnClickListener(listener)
    }

    var title: CharSequence
        get() {
            return unlockTitleTextView?.text?.toString() ?: ""
        }
        set(value) {
            unlockTitleTextView?.text = value
        }

    fun setTitle(@StringRes textId: Int) {
        title = context.getString(textId)
    }

    var message: CharSequence?
        get() {
            return unlockMessageTextView?.text?.toString() ?: ""
        }
        set(value) {
            if (value == null || value.isEmpty())
                unlockMessageTextView?.visibility = GONE
            else
                unlockMessageTextView?.visibility = VISIBLE
            unlockMessageTextView?.text = value ?: ""
        }

    fun setMessage(@StringRes textId: Int) {
        message = context.getString(textId)
    }

}