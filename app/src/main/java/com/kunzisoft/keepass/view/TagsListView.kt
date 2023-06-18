package com.kunzisoft.keepass.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.generateViewId
import androidx.core.view.children
import androidx.core.view.isGone
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.StubAnimatorListener
import com.kunzisoft.keepass.utils.dp

class TagsListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    var textColor: Int? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            expandBtn?.setColorFilter(value ?: Color.TRANSPARENT)
        }
    var bgColor: Int? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
        }
    var currentTags: List<String> = emptyList()
        set(value) {
            field = value
            drawAllTagsAndMeasure()
        }

    private var flow: Flow? = null
    private var expandBtn: AppCompatImageView? = null
    private var hiddenViews: MutableList<View> = mutableListOf()
    private var currentState: State = State.IDLE
    private var animationHelper: AnimationHelper? = null

    init {
        inflate(context, R.layout.tags_list_view, this)
        initialize()
    }

    private fun initialize() {
        viewTreeObserver.addOnGlobalLayoutListener(InitialMeasuringObserver())
        flow = findViewById(R.id.flow)
        expandBtn = findViewById<AppCompatImageView>(R.id.button)
        expandBtn?.setOnClickListener {
            animationHelper?.startAnimation()
            val sign = if (currentState == State.EXPANDED) -1 else 1
            it.animate().rotationBy(180f * sign).start()
        }
    }

    private fun drawAllTagsAndMeasure() {
        clear()
        post {
            layoutParams.height = WRAP_CONTENT
            currentState = State.MEASURING_EXPANDED
            makeTagsList()
        }
    }

    private fun clear() {
        for (child in children.toList()) {
            if (child.id == R.id.flow || child.id == R.id.button) continue
            removeView(child)
            flow?.removeView(child)
        }
        hiddenViews.clear()
    }

    private fun makeTagsList() {
        for (i in currentTags.indices) {
            val view = createTagView(currentTags[i])
            addView(view)
            if (i >= MAX_TAGS_IN_COLLAPSED) {
                hiddenViews.add(view)
            }
            flow?.addView(view)
        }
    }

    private fun toggleHiddenViews(animate: Boolean) {
        for (ind in hiddenViews.indices) {
            toggleHiddenView(ind, animate)
        }
    }

    private fun toggleHiddenView(ind: Int, animate: Boolean) {
        val isGone = hiddenViews[ind].isGone
        val alpha = if (isGone) 1f else 0f
        if (!animate) {
            hiddenViews[ind].isGone = !isGone
            hiddenViews[ind].alpha = alpha
            return
        }

        if (isGone) {
            hiddenViews[ind].isGone = !isGone
        }
        hiddenViews[ind].animate().setListener(object : StubAnimatorListener() {
            override fun onAnimationEnd(p0: Animator?) {
                if (!isGone) {
                    hiddenViews[ind].isGone = !isGone
                }
                requestLayout()
            }
        }).alpha(alpha).start()
    }

    private inner class AnimationHelper(
        expandedHeight: Int,
        collapsedHeight: Int,
    ) : StubAnimatorListener() {

        private val collapsingAnimator = setupAnimator(expandedHeight, collapsedHeight)
        private val expandingAnimator = setupAnimator(collapsedHeight, expandedHeight)

        fun startAnimation() {
            when (currentState) {
                State.EXPANDED -> animateInternal(collapsingAnimator)
                State.COLLAPSED -> animateInternal(expandingAnimator)
                else -> { /* np-op */ }
            }
        }

        private fun animateInternal(animator: Animator) {
            AnimatorSet().apply {
                play(animator)
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }

        override fun onAnimationStart(p0: Animator?) {
            if (currentState == State.COLLAPSED) return
            toggleHiddenViews(false)
        }

        override fun onAnimationEnd(p0: Animator?) {
            currentState = currentState.next()
            if (currentState == State.EXPANDED) {
                toggleHiddenViews(true)
            }
        }

        private fun setupAnimator(from: Int, to: Int): Animator {
            val animator = ValueAnimator.ofInt(from, to)
            animator.duration = ANIMATION_DURATION
            animator.addUpdateListener { animation ->
                post {
                    layoutParams.height = animation.animatedValue as Int
                    requestLayout()
                }
            }
            animator.addListener(this)
            return animator
        }
    }

    private inner class InitialMeasuringObserver : ViewTreeObserver.OnGlobalLayoutListener {
        private var expandedHeight = 0

        override fun onGlobalLayout() {
            when (currentState) {
                State.MEASURING_EXPANDED -> {
                    expandedHeight = measuredHeight
                    currentState = currentState.next()
                    toggleHiddenViews(false)
                }
                State.MEASURING_COLLAPSED -> {
                    currentState = currentState.next()
                    animationHelper = AnimationHelper(expandedHeight, measuredHeight)
                }
                else -> { /* no-op */ }
            }
        }
    }

    private enum class State {
        MEASURING_EXPANDED {
            override fun next() = MEASURING_COLLAPSED
        },
        MEASURING_COLLAPSED {
            override fun next() = COLLAPSED
        },
        EXPANDED {
            override fun next() = COLLAPSED
        },
        COLLAPSED {
            override fun next() = EXPANDED
        },
        IDLE {
            override fun next() = MEASURING_EXPANDED
        };

        abstract fun next(): State
    }

    private companion object {
        const val MAX_TAGS_IN_COLLAPSED = 4
        const val ANIMATION_DURATION = 300L
    }
}

private val VERTICAL_PADDING = 2.dp.intPx
private val HORIZONTAL_PADDING = 5.dp.intPx
private const val TAG_TEXT_SIZE = 13f
private val TAG_STROKE = 1.2f.dp.intPx

private fun TagsListView.createTagView(tag: String): View {
    val view = AppCompatTextView(context)
    view.text = tag
    view.id = generateViewId()
    return styleTagView(view)
}

private fun TagsListView.styleTagView(view: AppCompatTextView): View {
    val bg = createTagBg()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        view.background = bg
    } else {
        view.setBackgroundDrawable(bg)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        view.setTextAppearance(R.style.KeepassDXStyle_TextAppearance_Entry_Meta)
    } else {
        view.setTextAppearance(context, R.style.KeepassDXStyle_TextAppearance_Entry_Meta)
    }

    textColor?.let {
        view.setTextColor(it)
    }

    view.setPadding(HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING)
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAG_TEXT_SIZE)

    return view
}

private fun TagsListView.createTagBg(): Drawable? {
    val bg = ContextCompat.getDrawable(
        context,
        R.drawable.background_rounded_hollow_square,
    ) as? GradientDrawable

    bgColor?.let {
        bg?.setStroke(TAG_STROKE, it)
    }

    return bg
}