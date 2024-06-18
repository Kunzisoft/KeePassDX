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
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.helper.getLocalizedMessage
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable


/**
 * Replace font by monospace, must be called after setText()
 */
fun TextView.applyFontVisibility() {
    val typeFace = Typeface.createFromAsset(context.assets, "fonts/FiraMono-Regular.ttf")
    typeface = typeFace
}

fun TextView.applyHiddenStyle(hide: Boolean, changeMaxLines: Boolean = true) {
    if (hide) {
        transformationMethod = PasswordTransformationMethod.getInstance()
        if (changeMaxLines)
            maxLines = 1
    } else {
        transformationMethod = null
        if (changeMaxLines)
            maxLines = 800
    }
}

fun TextView.setTextSize(unit: Int, defaultSize: Float, multiplier: Float) {
    if (multiplier > 0.0F)
        setTextSize(unit, defaultSize * multiplier)
}

fun TextView.strikeOut(strikeOut: Boolean) {
    paintFlags = if (strikeOut)
        paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    else
        paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

fun TextView.customLink(listener: (View) -> Unit) {
    val spannableString = SpannableString(this.text)
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(view: View) {
            Selection.setSelection((view as TextView).text as Spannable, 0)
            view.invalidate()
            listener.invoke(view)
        }
    }
    spannableString.setSpan(clickableSpan, 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    this.movementMethod = LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
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
    slideAnimator.duration = if (animate) 300L else 0L
    slideAnimator.addUpdateListener { animation ->
        layoutParams.height = animation.animatedValue as Int
        requestLayout()
    }
    AnimatorSet().apply {
        play(slideAnimator)
        interpolator = AccelerateDecelerateInterpolator()
        addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                layoutParams.height = recordViewHeight
                onCollapseFinished?.invoke()
            }
            override fun onAnimationCancel(animation: Animator) {}
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
    slideAnimator.duration = if (animate) 300L else 0L
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
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                onExpandFinished?.invoke()
            }
            override fun onAnimationCancel(animation: Animator) {}
        })
    }.start()
}

/***
 * This function returns the actual height the layout.
 * The getHeight() function returns the current height which might be zero if
 * the layout's visibility is GONE
 */
fun ViewGroup.getFullHeight(): Int {
    measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val initialVisibility = visibility
    visibility = LinearLayout.VISIBLE
    val desiredWidth = View.MeasureSpec.makeMeasureSpec(
        width,
        View.MeasureSpec.AT_MOST
    )
    measure(desiredWidth, View.MeasureSpec.UNSPECIFIED)
    val totalHeight = measuredHeight
    visibility = initialVisibility
    return totalHeight
}

fun View.hideByFading() {
    alpha = 1f
    animate()
            .alpha(0f)
            .setDuration(140)
            .setListener(object: Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {}
                override fun onAnimationEnd(p0: Animator) {
                    isVisible = false
                }
                override fun onAnimationCancel(p0: Animator) {}
                override fun onAnimationRepeat(p0: Animator) {}
            })
}

fun View.showByFading() {
    if (!isVisible) {
        isVisible = true
        // Trick to keep the focus
        alpha = 0.0001f
        animate()
            .alpha(1f)
            .setDuration(140)
            .setListener(null)
    }
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

fun Context.showActionErrorIfNeeded(result: ActionRunnable.Result) {
    if (!result.isSuccess) {
        result.exception?.getLocalizedMessage(resources)?.let { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        } ?: result.message?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}

fun CoordinatorLayout.showActionErrorIfNeeded(result: ActionRunnable.Result) {
    if (!result.isSuccess) {
        result.exception?.getLocalizedMessage(resources)?.let { errorMessage ->
            Snackbar.make(this, errorMessage, Snackbar.LENGTH_LONG).asError().show()
        } ?: result.message?.let { message ->
            Snackbar.make(this, message, Snackbar.LENGTH_LONG).asError().show()
        }
    }
}

fun Toolbar.changeControlColor(color: Int) {
    val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    for (i in 0 until childCount) {
        val view: View = getChildAt(i)
        // Change the color of back button (or open drawer button).
        if (view is ImageView) {
            //Action Bar back button
            view.drawable.colorFilter = colorFilter
        }
        if (view is ActionMenuView) {
            view.post {
                for (j in 0 until view.childCount) {
                    // Change the color of any ActionMenuViews - icons that
                    // are not back button, nor text, nor overflow menu icon.
                    val innerView: View = view.getChildAt(j)
                    if (innerView is ActionMenuItemView) {
                        innerView.compoundDrawables.forEach { drawable ->
                            //Important to set the color filter in separate thread,
                            //by adding it to the message queue
                            //Won't work otherwise.
                            drawable?.colorFilter = colorFilter
                        }
                    }
                }
            }
        }
    }
    // Change the color of title and subtitle.
    setTitleTextColor(color)
    setSubtitleTextColor(color)
    // Change the color of the Overflow Menu icon.
    var drawable: Drawable? = overflowIcon
    if (drawable != null) {
        drawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(drawable.mutate(), color)
        overflowIcon = drawable
    }
    invalidate()
}

fun CollapsingToolbarLayout.changeTitleColor(color: Int) {
    setCollapsedTitleTextColor(color)
    setExpandedTitleColor(color)
    invalidate()
}

fun Activity.setTransparentNavigationBar(applyToStatusBar: Boolean = false, applyWindowInsets: () -> Unit) {
    // Only in portrait
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.surface_selector)
        if (applyToStatusBar) {
            obtainStyledAttributes(intArrayOf(R.attr.colorSurface)).apply {
                window.statusBarColor = getColor(0, Color.GRAY)
                recycle()
            }
        }
        applyWindowInsets.invoke()
    }
}

/**
 * Apply a margin to a view to fix the window inset
 */
fun View.applyWindowInsets(position: WindowInsetPosition = WindowInsetPosition.BOTTOM) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        var consumed = false

        // To fix listener in API 27
        if (view is ViewGroup) {
            view.forEach { child ->
                // Dispatch the insets to the child
                val childResult = ViewCompat.dispatchApplyWindowInsets(child, windowInsets)
                // If the child consumed the insets, record it
                if (childResult.isConsumed) {
                    consumed = true
                }
            }
        }

        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        when (position) {
            WindowInsetPosition.TOP -> {
                if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = insets.top
                    }
                }
            }
            WindowInsetPosition.LEGIT_TOP -> {
                if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = 0
                    }
                }
            }
            WindowInsetPosition.BOTTOM -> {
                if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = insets.bottom
                    }
                }
            }
            WindowInsetPosition.BOTTOM_IME -> {
                val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = if (imeHeight > 1) 0 else insets.bottom
                    }
                }
            }
            WindowInsetPosition.TOP_BOTTOM_IME -> {
                val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        topMargin = insets.top
                        bottomMargin = if (imeHeight > 1) imeHeight else 0
                    }
                }
            }
        }
        // If any of the children consumed the insets, return an appropriate value
        if (consumed) WindowInsetsCompat.CONSUMED else windowInsets
    }
}

enum class WindowInsetPosition {
    TOP, BOTTOM, LEGIT_TOP, BOTTOM_IME, TOP_BOTTOM_IME
}
