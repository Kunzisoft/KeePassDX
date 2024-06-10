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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.RelativeLayout

import com.kunzisoft.keepass.R

class AddNodeButtonView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyle: Int = 0)
    : RelativeLayout(context, attrs, defStyle) {

    var addButtonView: FloatingActionButton? = null
    private lateinit var addEntryView: View
    private lateinit var fabAddEntryView: View
    private lateinit var addGroupView: View
    private lateinit var fabAddGroupView: View

    private var addEntryEnable: Boolean = false
    private var addGroupEnable: Boolean = false

    private var state: State? = null
    private var allowAction: Boolean = false
    private var onAddButtonClickListener: OnClickListener? = null
    private var onAddButtonVisibilityChangedListener: FloatingActionButton.OnVisibilityChangedListener? = null
    private var viewButtonMenuAnimation: AddButtonAnimation? = null
    private var viewMenuAnimationAddEntry: ViewMenuAnimation? = null
    private var viewMenuAnimationAddGroup: ViewMenuAnimation? = null
    private var animationDuration: Long = 0

    val isEnable: Boolean
        get() = visibility == View.VISIBLE

    private enum class State {
        OPEN, CLOSE
    }

    init {
        inflate(context)
        hideButton()
    }

    private fun inflate(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_button_add_node, this)

        addEntryEnable = true
        addGroupEnable = true

        addButtonView = findViewById(R.id.add_button)
        addEntryView = findViewById(R.id.container_add_entry)
        fabAddEntryView = findViewById(R.id.fab_add_entry)
        addGroupView = findViewById(R.id.container_add_group)
        fabAddGroupView = findViewById(R.id.fab_add_group)

        animationDuration = 300L

        viewButtonMenuAnimation = AddButtonAnimation(addButtonView)
        viewMenuAnimationAddEntry = ViewMenuAnimation(addEntryView, 150L, 0L)
        viewMenuAnimationAddGroup = ViewMenuAnimation(addGroupView, 0L, 150L)

        allowAction = true
        state = State.CLOSE

        onAddButtonClickListener = OnClickListener{ startGlobalAnimation() }
        addButtonView?.setOnClickListener(onAddButtonClickListener)

        onAddButtonVisibilityChangedListener = object : FloatingActionButton.OnVisibilityChangedListener() {
            override fun onHidden(fab: FloatingActionButton?) {
                super.onHidden(fab)
                addButtonView?.setOnClickListener(null)
                addButtonView?.isClickable = false
            }

            override fun onShown(fab: FloatingActionButton?) {
                super.onShown(fab)
                addButtonView?.setOnClickListener(onAddButtonClickListener)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val viewButtonRect = Rect()
        val viewEntryRect = Rect()
        val viewGroupRect = Rect()
        addButtonView?.getGlobalVisibleRect(viewButtonRect)
        addEntryView.getGlobalVisibleRect(viewEntryRect)
        addGroupView.getGlobalVisibleRect(viewGroupRect)
        if (!(viewButtonRect.contains(event.rawX.toInt(), event.rawY.toInt())
                        && viewEntryRect.contains(event.rawX.toInt(), event.rawY.toInt())
                        && viewGroupRect.contains(event.rawX.toInt(), event.rawY.toInt()))) {
            closeButtonIfOpen()
        }
        return super.onTouchEvent(event)
    }

    fun hideOrShowButtonOnScrollListener(dy: Int) {
        if (state == State.CLOSE) {
            if (dy > 0 && addButtonView?.visibility == View.VISIBLE) {
                hideButton()
            } else if (dy < 0 && addButtonView?.visibility != View.VISIBLE) {
                showButton()
            }
        }
    }

    fun showButton() {
        if (isEnable && addButtonView?.visibility != VISIBLE)
            addButtonView?.show(onAddButtonVisibilityChangedListener)
    }

    fun hideButton() {
        if (addButtonView?.visibility == VISIBLE)
            addButtonView?.hide(onAddButtonVisibilityChangedListener)
    }

    /**
     * Start the animation to close the button
     */
    fun openButtonIfClose() {
        if (state == State.CLOSE) {
            startGlobalAnimation()
        }
    }

    /**
     * Start the animation to close the button
     */
    fun closeButtonIfOpen() {
        if (state == State.OPEN) {
            startGlobalAnimation()
        }
    }

    /**
     * Enable or not the possibility to add an entry by pressing a button
     * @param enable true to enable
     */
    fun enableAddEntry(enable: Boolean) {
        this.addEntryEnable = enable
        if (enable && addEntryView.visibility != View.VISIBLE)
            addEntryView.visibility = View.INVISIBLE
        disableViewIfNoAddAvailable()
    }

    /**
     * Enable or not the possibility to add a group by pressing a button
     * @param enable true to enable
     */
    fun enableAddGroup(enable: Boolean) {
        this.addGroupEnable = enable
        if (enable && addGroupView.visibility != View.VISIBLE)
            addGroupView.visibility = View.INVISIBLE
        disableViewIfNoAddAvailable()
    }

    private fun disableViewIfNoAddAvailable() {
        visibility = if (!addEntryEnable && !addGroupEnable) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun onButtonClickListener(onClickListener: OnClickListener) =
        OnClickListener { view ->
            onClickListener.onClick(view)
            closeButtonIfOpen()
        }

    fun setAddGroupClickListener(onClickListener: OnClickListener) {
        if (addGroupEnable) {
            fabAddGroupView.setOnClickListener(onButtonClickListener(onClickListener))
        }
    }

    fun setAddEntryClickListener(onClickListener: OnClickListener) {
        if (addEntryEnable) {
            fabAddEntryView.setOnClickListener(onButtonClickListener(onClickListener))
        }
    }

    private fun startGlobalAnimation() {
        if (allowAction) {
            viewButtonMenuAnimation?.startAnimation()

            if (addEntryEnable) {
                viewMenuAnimationAddEntry?.startAnimation()
            }
            if (addGroupEnable) {
                viewMenuAnimationAddGroup?.startAnimation()
            }
        }
    }

    private inner class AddButtonAnimation(private val view: View?) : ViewPropertyAnimatorListener {
        private var isRotate: Boolean = false

        private val interpolator: Interpolator

        init {
            this.isRotate = false
            interpolator = AccelerateDecelerateInterpolator()
        }

        override fun onAnimationStart(view: View) {
            allowAction = false
        }

        override fun onAnimationEnd(view: View) {
            allowAction = true
            isRotate = !isRotate
            state = if (isRotate)
                State.OPEN
            else
                State.CLOSE
        }

        override fun onAnimationCancel(view: View) {}

        fun startAnimation() {
            view?.let { view ->
                if (!isRotate) {
                    ViewCompat.animate(view)
                            .rotation(135.0f)
                            .withLayer()
                            .setDuration(animationDuration)
                            .setInterpolator(interpolator)
                            .setListener(this)
                            .start()
                } else {
                    ViewCompat.animate(view)
                            .rotation(0.0f)
                            .withLayer()
                            .setDuration(animationDuration)
                            .setInterpolator(interpolator)
                            .setListener(this)
                            .start()
                }
            }
        }
    }

    private inner class ViewMenuAnimation(private val view: View?,
                                          private val delayIn: Long,
                                          private val delayOut: Long)
        : ViewPropertyAnimatorListener {

        private val interpolator: Interpolator
        private var translation: Float = 0.toFloat()
        private var wasInvisible: Boolean = false

        init {
            this.interpolator = FastOutSlowInInterpolator()
            this.wasInvisible = true
            this.translation = 0f
        }

        override fun onAnimationStart(view: View) {}

        override fun onAnimationEnd(view: View) {
            if (wasInvisible) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.INVISIBLE
            }
        }

        override fun onAnimationCancel(view: View) {}

        fun startAnimation() {
            view?.let { view ->
               if (view.visibility == View.VISIBLE) {
                    // In
                    wasInvisible = false
                    ViewCompat.animate(view)
                            .translationY(-translation)
                            .translationX((view.width / 3).toFloat())
                            .alpha(0.0f)
                            .scaleX(0.33f)
                            .setDuration(animationDuration - delayIn)
                            .setInterpolator(interpolator)
                            .setListener(this)
                            .setStartDelay(delayIn)
                            .start()
                } else {

                // The first time
                if (translation == 0f) {
                    if (addButtonView != null) {
                        translation = view.y + view.height / 2 - addButtonView!!.y - (addButtonView!!.height / 2).toFloat()
                        view.apply {
                            translationY = -translation
                            translationX = (view.width / 3).toFloat()
                            alpha = 0.0f
                            scaleX = 0.33f
                        }
                    }
                }

                // Out
                view.visibility = View.VISIBLE
                wasInvisible = true
                ViewCompat.animate(view)
                        .translationY(1f)
                        .translationX(1f)
                        .alpha(1.0f)
                        .scaleX(1f)
                        .setDuration(animationDuration - delayOut)
                        .setInterpolator(interpolator)
                        .setListener(this)
                        .setStartDelay(delayOut)
                        .start()
                }
            }
        }
    }
}
