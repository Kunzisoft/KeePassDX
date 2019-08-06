package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.fingerprint.FingerPrintAnimatedVector

class FingerPrintInfoView @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyle: Int = 0)
    : LinearLayout(context, attrs) {

    private val fingerPrintContainerView: View
    private var fingerPrintAnimatedVector: FingerPrintAnimatedVector? = null
    private var fingerPrintTextView: TextView? = null
    var fingerPrintImageView: ImageView? = null

    init {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.fingerprint_show, this)

        fingerPrintContainerView = findViewById(R.id.fingerprint_container)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerPrintTextView = findViewById(R.id.fingerprint_label)
            fingerPrintImageView = findViewById(R.id.fingerprint_image)
            // Init the fingerprint animation
            fingerPrintAnimatedVector = FingerPrintAnimatedVector(context, fingerPrintImageView!!)
        }
    }

    fun startFingerPrintAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerPrintAnimatedVector?.startScan()
        }
    }

    fun stopFingerPrintAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerPrintAnimatedVector?.stopScan()
        }
    }

    var text: CharSequence
        get() {
            return fingerPrintTextView?.text?.toString() ?: ""
        }
        set(value) {
            fingerPrintTextView?.text = value
        }

    fun setText(textId: Int, lock: Boolean = false) {
        setText(context.getString(textId), lock)
    }

    fun setText(text: CharSequence, lock: Boolean = false) {
        fingerPrintContainerView.alpha = if (lock) 0.8f else 1f
        fingerPrintTextView?.text = text
    }

    var hide: Boolean
        get() {
            return visibility != VISIBLE
        }
        set(value) {
            visibility = if (value) View.GONE else View.VISIBLE
        }

}