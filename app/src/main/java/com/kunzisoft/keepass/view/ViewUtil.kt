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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable

/**
 * Replace font by monospace, must be called after seText()
 */
fun TextView.applyFontVisibility() {
    val typeFace = Typeface.createFromAsset(context.assets, "fonts/FiraMono-Regular.ttf")
    typeface = typeFace
}

fun TextView.applyHiddenStyle(hide: Boolean) {
    if (hide) {
        transformationMethod = PasswordTransformationMethod.getInstance()
        maxLines = 1
    } else {
        transformationMethod = null
        maxLines = 800
    }
}

fun TextView.setTextSize(unit: Int, defaultSize: Float, multiplier: Float) {
    if (multiplier > 0.0F && multiplier != 1.0F)
        setTextSize(unit, defaultSize * multiplier)
}

fun TextView.strikeOut(strikeOut: Boolean) {
    paintFlags = if (strikeOut)
        paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    else
        paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

fun Snackbar.asError(): Snackbar {
    this.view.apply {
        setBackgroundColor(Color.RED)
        findViewById<TextView>(R.id.snackbar_text).setTextColor(Color.WHITE)
    }
    return this
}

fun View.collapse(animate: Boolean = true,
                  onCollapseFinished: (() -> Unit)? = null) {
    val recordViewHeight = layoutParams.height
    val slideAnimator = ValueAnimator.ofInt(height, 0)
    if (animate)
        slideAnimator.duration = 300L
    slideAnimator.addUpdateListener { animation ->
        layoutParams.height = animation.animatedValue as Int
        requestLayout()
    }
    AnimatorSet().apply {
        play(slideAnimator)
        interpolator = AccelerateDecelerateInterpolator()
        addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                visibility = View.GONE
                layoutParams.height = recordViewHeight
                onCollapseFinished?.invoke()
            }
            override fun onAnimationCancel(animation: Animator?) {}
        })
    }.start()
}

fun View.expand(animate: Boolean = true,
                defaultHeight: Int? = null,
                onExpandFinished: (() -> Unit)? = null)  {
    val viewHeight = defaultHeight ?: layoutParams.height
    layoutParams.height = 0
    val slideAnimator = ValueAnimator
            .ofInt(0, viewHeight)
    if (animate)
        slideAnimator.duration = 300L
    var alreadyVisible = false
    slideAnimator.addUpdateListener { animation ->
        layoutParams.height = animation.animatedValue as Int
        if (!alreadyVisible && layoutParams.height > 0) {
            visibility = View.VISIBLE
            alreadyVisible = true
        }
        requestLayout()
    }
    AnimatorSet().apply {
        play(slideAnimator)
        interpolator = AccelerateDecelerateInterpolator()
        addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                onExpandFinished?.invoke()
            }
            override fun onAnimationCancel(animation: Animator?) {}
        })
    }.start()
}

fun View.updateLockPaddingLeft() {
    updatePadding(resources.getDimensionPixelSize(
            if (PreferencesUtil.showLockDatabaseButton(context)) {
                R.dimen.lock_button_size
            } else {
                R.dimen.hidden_lock_button_size
            }
    ))
}

fun CoordinatorLayout.showActionError(result: ActionRunnable.Result) {
    if (!result.isSuccess) {
        result.exception?.errorId?.let { errorId ->
            Snackbar.make(this, errorId, Snackbar.LENGTH_LONG).asError().show()
        } ?: result.message?.let { message ->
            Snackbar.make(this, message, Snackbar.LENGTH_LONG).asError().show()
        }
    }
}