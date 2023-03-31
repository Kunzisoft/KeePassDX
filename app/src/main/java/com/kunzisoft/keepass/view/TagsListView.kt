package com.kunzisoft.keepass.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.generateViewId
import androidx.core.view.children
import androidx.core.view.isGone
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.dp
import kotlin.math.exp

class TagsListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.tags_list_view, this)
        initialize()
    }

    private var measureingCollapsedHeight: Boolean = false

    var textColor: Int? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
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
    private var expandBtn: View? = null
    private var expanded: Boolean = false
    private var expandCllbacks: List<(() -> Unit)> = emptyList()
    private var measureingExpandedHeight = false
    private var hiddenViews: MutableList<View> = mutableListOf()
    private var initialLayoutParams: ViewGroup.LayoutParams? = null
    private var capturingInitialLp = true

    private var expandedHeight: Int = 0
    private var collapsedHeight: Int = 0

    private fun initialize() {
        viewTreeObserver.addOnGlobalLayoutListener(Observer())
        flow = findViewById(R.id.flow)
        expandBtn = findViewById<AppCompatImageView>(R.id.button)
        expandBtn?.setOnClickListener {
            expanded = !expanded
            animateTagsChange(collapsedHeight, expandedHeight)
            it.animate().rotationBy(180f).start()
        }
    }

    private fun clear() {
        for (child in children.toList()) {
            if (child == flow || child.id == R.id.button) continue
            removeView(child)
            flow?.removeView(child)
        }
        hiddenViews.clear()
    }

    private fun drawAllTagsAndMeasure() {
        clear()
        layoutParams?.apply {
            height = WRAP_CONTENT
        }
        post {
            measureingExpandedHeight = true
            makeTagsList(false)
        }
    }

    private fun makeTagsList(hideExtra: Boolean) {
        for (i in currentTags.indices) {
            val view = createTagView(currentTags[i])
            addView(view)
            if (i >= MAX_TAGS_IN_COLLAPSED) {
                hiddenViews.add(view)
            }
            flow?.addView(view)
        }
    }

    private fun hideViews() {
        hiddenViews.forEach {
            it.isGone = true
            it.alpha = 0f
        }
    }

    private fun calculateUpperBound(): Int {
        return when {
            expanded -> currentTags.size
            else -> currentTags.size
        }
    }

    private fun animateTagsChange(from: Int, to: Int) {
        if (expanded) {
            animateExpand(from, to)
        } else {
            animateExpand(to, from)
        }
    }

    private fun animateExpand(from: Int, to: Int) {
        val slideAnimator = ValueAnimator.ofInt(from, to)
        slideAnimator.duration = 300L
        slideAnimator.addUpdateListener { animation ->
            layoutParams.height = animation.animatedValue as Int
            requestLayout()
        }
        slideAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
                if (!expanded) {
                    hiddenViews.forEach {
                        it.isGone = true
                        it.animate().alpha(0f).start()
                    }
                }
            }

            override fun onAnimationEnd(p0: Animator?) {
                if (expanded) {
                    hiddenViews.forEach {
                        it.isGone = false
                        it.animate().alpha(1f).start()
                    }
                }
            }
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationRepeat(p0: Animator?) {}

        })

        AnimatorSet().apply {
            play(slideAnimator)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private inner class Observer : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measureingExpandedHeight) {
                measureingExpandedHeight = false
                Log.d("DAMN", expandedHeight.toString())
                expandedHeight = measuredHeight
                Log.d("DAMN", measuredHeight.toString())
                Log.d("DAMN", children.filter { it is AppCompatTextView }.map { "(${it.x}, ${it.y})" }.toList().toString())
                post {
                    measureingCollapsedHeight = true
                    hideViews()
                }
            }
            if (measureingCollapsedHeight) {
                measureingCollapsedHeight = false
                collapsedHeight = measuredHeight
            }
        }
    }

    companion object {
        private const val MAX_TAGS_IN_COLLAPSED = 4
    }
}

private val VERTICAL_PADDING = 2.dp.intPx
private val HORIZONTAL_PADDING = 5.dp.intPx

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
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

    return view
}

private fun TagsListView.createTagBg(): Drawable? {
    val bg = ContextCompat.getDrawable(
        context,
        R.drawable.background_rounded_hollow_square,
    ) as? GradientDrawable

    bgColor?.let {
        bg?.setStroke(1.2f.dp.intPx, it)
    }

    return bg
}