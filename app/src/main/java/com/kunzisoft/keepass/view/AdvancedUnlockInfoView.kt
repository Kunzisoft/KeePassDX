package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.biometric.FingerPrintAnimatedVector

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

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_advanced_unlock, this)

        unlockContainerView = findViewById(R.id.fingerprint_container)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            unlockTitleTextView = findViewById(R.id.biometric_title)
            unlockMessageTextView = findViewById(R.id.biometric_message)
            unlockIconImageView = findViewById(R.id.biometric_image)
            // Init the fingerprint animation
            unlockAnimatedVector = FingerPrintAnimatedVector(context, unlockIconImageView!!)
        }
    }

    fun startIconViewAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            unlockAnimatedVector?.startScan()
        }
    }

    fun stopIconViewAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            unlockAnimatedVector?.stopScan()
        }
    }

    fun setIconViewClickListener(listener: ((view: View)->Unit)?) {
        if (listener == null)
            stopIconViewAnimation()
        else
            startIconViewAnimation()
        unlockContainerView.alpha = if (listener == null) 0.8f else 1f
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

    var message: CharSequence
        get() {
            return unlockMessageTextView?.text?.toString() ?: ""
        }
        set(value) {
            unlockMessageTextView?.text = value
        }

    fun setMessage(@StringRes textId: Int) {
        message = context.getString(textId)
    }

    var hide: Boolean
        get() {
            return visibility != VISIBLE
        }
        set(value) {
            visibility = if (value) View.GONE else View.VISIBLE
        }

}