package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.generateViewId
import androidx.core.view.children
import androidx.core.view.updatePadding
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.utils.dp
import com.kunzisoft.keepass.utils.sp

class TagsListView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    init {
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

    private var flow: Flow? = null
    private var dotsView: View? = null
    private var expanded: Boolean = false

    var currentTags: List<String> = emptyList()
        set(value) {
            field = value
            clear()
            makeTagsList()
        }

    private fun initialize() {
        flow = Flow(context).apply {
            setWrapMode(Flow.WRAP_CHAIN)
            setHorizontalAlign(Flow.HORIZONTAL_ALIGN_START)
            setHorizontalStyle(Flow.CHAIN_PACKED)
            setHorizontalGap(4.dp.intPx)
            setVerticalGap(2.dp.intPx)
            setHorizontalBias(0f)
            setVerticalBias(0f)
        }
        addView(flow)
        dotsView = createTagView("...")
        dotsView?.setOnClickListener {
            clear()
            expanded = !expanded
            makeTagsList()
        }
    }

    private fun styleDotsView() {
        val view = dotsView as? AppCompatTextView ?: return
        styleTagView(view)
    }

    private fun clear() {
        for (child in children.toList()) {
            if (child == flow) continue
            removeView(child)
            flow?.removeView(child)
        }
    }

    private fun makeTagsList() {
        val upperBound = calculateUpperBound()
        val showDots = shouldShowDots()
        val tags = currentTags.subList(0, upperBound)

        for (i in tags.indices) {
            val view = createTagView(tags[i])
            addView(view)
            flow?.addView(view)
        }

        if (showDots) {
            addView(dotsView)
            flow?.addView(dotsView)
        }
    }

    private fun calculateUpperBound(): Int {
        return when {
            expanded -> currentTags.size
            currentTags.size > MAX_TAGS_IN_COLLAPSED -> MAX_TAGS_IN_COLLAPSED
            else -> currentTags.size
        }
    }

    private fun shouldShowDots(): Boolean {
        return currentTags.size > MAX_TAGS_IN_COLLAPSED
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
