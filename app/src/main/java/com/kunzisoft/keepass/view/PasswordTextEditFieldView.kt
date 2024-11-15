package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kunzisoft.keepass.password.PasswordEntropy


class PasswordTextEditFieldView @JvmOverloads constructor(context: Context,
                                                          attrs: AttributeSet? = null,
                                                          defStyle: Int = 0)
    : TextEditFieldView(context, attrs, defStyle) {

    private var passwordProgressViewId = ViewCompat.generateViewId()
    private var passwordEntropyViewId = ViewCompat.generateViewId()

    private var mPasswordProgress = LinearProgressIndicator(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT).apply {
                addRule(ALIGN_PARENT_BOTTOM)
            }
        setPadding(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                context.resources.displayMetrics
            ).toInt()
        )
        setIndicatorColor(PasswordEntropy.Strength.RISKY.color)
        progress = 0
        max = 100
    }

    private val mPasswordEntropyView = TextView(context).apply {
        LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT).apply {
                addRule(ALIGN_PARENT_BOTTOM or ALIGN_PARENT_RIGHT)
            }
    }

    private var mPasswordEntropyCalculator: PasswordEntropy = PasswordEntropy {
        valueView.text?.toString()?.let { firstPassword ->
            getEntropyStrength(firstPassword)
        }
    }

    init {
        buildViews()

        addView(mPasswordProgress)
        addView(mPasswordEntropyView)
    }

    private fun buildViews() {
        mPasswordProgress.apply {
            id = passwordProgressViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, actionImageButtonId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it.addRule(START_OF, actionImageButtonId)
                }
            }
        }
        mPasswordEntropyView.apply {
            id = passwordEntropyViewId
            layoutParams = (layoutParams as LayoutParams?)?.also {
                it.addRule(LEFT_OF, actionImageButtonId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    it.addRule(START_OF, actionImageButtonId)
                }
            }
        }
    }

    private fun getEntropyStrength(passwordText: String) {
        mPasswordEntropyCalculator.getEntropyStrength(passwordText) { entropyStrength ->
            mPasswordProgress.apply {
                post {
                    setIndicatorColor(entropyStrength.strength.color)
                    setProgressCompat(entropyStrength.estimationPercent, true)
                }
            }
            mPasswordEntropyView.apply {
                post {
                    text = PasswordEntropy.getStringEntropy(resources, entropyStrength.entropy)
                }
            }
        }
    }

    override var label: String
        get() {
            return super.label
        }
        set(value) {
            super.label = value
            // Define views Ids with label value
            passwordProgressViewId = "passwordProgressViewId $value".hashCode()
            passwordEntropyViewId = "passwordEntropyViewId $value".hashCode()
            buildViews()
        }
}