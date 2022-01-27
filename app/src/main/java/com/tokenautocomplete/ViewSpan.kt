package com.tokenautocomplete

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.text.style.ReplacementSpan
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange

/**
 * Span that holds a view it draws when rendering
 *
 * Created on 2/3/15.
 * @author mgod
 */
open class ViewSpan(var view: View, private val layout: Layout) : ReplacementSpan() {
    private var cachedMaxWidth = -1
    private fun prepView() {
        if (layout.maxViewSpanWidth != cachedMaxWidth || view.isLayoutRequested) {
            cachedMaxWidth = layout.maxViewSpanWidth
            var spec = View.MeasureSpec.AT_MOST
            if (cachedMaxWidth == 0) {
                //If the width is 0, allow the view to choose it's own content size
                spec = View.MeasureSpec.UNSPECIFIED
            }
            val widthSpec = View.MeasureSpec.makeMeasureSpec(cachedMaxWidth, spec)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
    }

    override fun draw(
        canvas: Canvas, text: CharSequence, @IntRange(from = 0) start: Int,
        @IntRange(from = 0) end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        prepView()
        canvas.save()
        canvas.translate(x, top.toFloat())
        view.draw(canvas)
        canvas.restore()
    }

    override fun getSize(
        paint: Paint, charSequence: CharSequence, @IntRange(from = 0) start: Int,
        @IntRange(from = 0) end: Int, fontMetricsInt: FontMetricsInt?
    ): Int {
        prepView()
        if (fontMetricsInt != null) {
            //We need to make sure the layout allots enough space for the view
            val height = view.measuredHeight
            var adjustedBaseline = view.baseline
            //-1 means the view doesn't support baseline alignment, so align bottom to font baseline
            if (adjustedBaseline == -1) {
                adjustedBaseline = height
            }
            fontMetricsInt.top = -adjustedBaseline
            fontMetricsInt.ascent = fontMetricsInt.top
            fontMetricsInt.bottom = height - adjustedBaseline
            fontMetricsInt.descent = fontMetricsInt.bottom
        }
        return view.right
    }

    interface Layout {
        val maxViewSpanWidth: Int
    }

    init {
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}