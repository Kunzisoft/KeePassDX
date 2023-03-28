package com.kunzisoft.keepass.view

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
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.generateViewId
import androidx.core.view.children
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.dp

class TagsListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.tags_list_view, this)
        initialize()
    }

    var textColor: Int? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            styleDotsView()
        }
    var bgColor: Int? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            styleDotsView()
        }
    var currentTags: List<String> = emptyList()
        set(value) {
            field = value
            clear()
            drawAllTagsAndMeasure()
            clear()
            makeTagsList(true)
        }

    private var flow: Flow? = null
    private var dotsView: View? = null
    private var expanded: Boolean = false
    private var cachedTopPadding: Int = 0
    private var expandCllbacks: List<(() -> Unit)> = emptyList()
    private var initialMeasure = false

    private fun initialize() {
        flow = findViewById(R.id.flow)
        dotsView = createTagView("...")

        findViewById<AppCompatImageView>(R.id.button)?.setOnClickListener {
            clear()
            expanded = !expanded
            makeTagsList()
            it.animate().rotationBy(180f).start()
            if (expanded) {
                //animateTagsChange(lastHeight, lastHeight + 30.dp.intPx)
                expandCllbacks.forEach { it() }
                expandCllbacks = emptyList()
            }
        }
    }

    private fun styleDotsView() {
        val view = dotsView as? AppCompatTextView ?: return
        styleTagView(view)
    }

    private fun clear() {
        for (child in children.toList()) {
            if (child == flow || child.id == R.id.button) continue
            removeView(child)
            flow?.removeView(child)
        }
    }

    private fun drawAllTagsAndMeasure() {
        initialMeasure = true
        val tags = currentTags
        for (i in tags.indices) {
            val view = createTagView(tags[i])
            addView(view)
            flow?.addView(view)
        }
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (initialMeasure) {
            initialMeasure = false
            cachedTopPadding = measuredHeight
            Log.d("DAMN", "Dfasfas $cachedTopPadding")
        }
    }

    private fun makeTagsList(initial: Boolean = false) {
        val lastHeight = measuredHeight
        val upperBound = calculateUpperBound()
        val tags = currentTags.subList(0, upperBound)
        val callbacks = mutableListOf<(() -> Unit)>()

        for (i in tags.indices) {
            val view = createTagView(tags[i])
            addView(view)
            if (i >= MAX_TAGS_IN_COLLAPSED) {
                view.alpha = 0f
                callbacks.add {
                    view.animate().alpha(1f).start()
                }
            }
            flow?.addView(view)
        }

        expandCllbacks = callbacks

        val currentHeight = measuredHeight
        requestLayout()
        Log.d("DAMN", "${lastHeight} ${currentHeight}")
    }

    private fun calculateUpperBound(): Int {
        return when {
            expanded -> currentTags.size
            currentTags.size > MAX_TAGS_IN_COLLAPSED -> MAX_TAGS_IN_COLLAPSED
            else -> currentTags.size
        }
    }

    private fun animateTagsChange(from: Int, to: Int) {
        if (expanded) {
            animateExpand(from, to)
        } else {
            animateCollapse(from, to)
        }
    }

    private fun animateExpand(from: Int, to: Int) {
        layoutParams.height = 0
        val slideAnimator = ValueAnimator.ofInt(from, to)
        slideAnimator.duration = 300L
        slideAnimator.addUpdateListener { animation ->
            layoutParams.height = animation.animatedValue as Int
            requestLayout()
        }

        val f = flow ?: return
        val slideAnimator2 = ValueAnimator.ofInt(cachedTopPadding, 0)
        slideAnimator2.duration = 248L
        slideAnimator2.addUpdateListener { animation ->
            f.paddingTop = animation.animatedValue as Int
            requestLayout()
        }
        AnimatorSet().apply {
            play(slideAnimator)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
        AnimatorSet().apply {
            play(slideAnimator2)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun animateCollapse(from: Int, to: Int) {
        layoutParams.height = 0
        val slideAnimator = ValueAnimator.ofInt(from, to)
        slideAnimator.duration = 300L
        slideAnimator.addUpdateListener { animation ->
            layoutParams.height = animation.animatedValue as Int
            requestLayout()
        }

        val f = flow ?: return
        val slideAnimator2 = ValueAnimator.ofInt(0, cachedTopPadding)
        slideAnimator2.duration = 300L
        slideAnimator2.addUpdateListener { animation ->
            f.paddingTop = animation.animatedValue as Int
            requestLayout()
        }
        AnimatorSet().apply {
            play(slideAnimator)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
        AnimatorSet().apply {
            play(slideAnimator2)
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
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