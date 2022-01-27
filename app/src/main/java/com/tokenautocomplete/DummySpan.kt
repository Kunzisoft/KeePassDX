package com.tokenautocomplete

import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * Invisible MetricAffectingSpan that will trigger a redraw when it is being added to or removed from an Editable.
 *
 * @see TokenCompleteTextView.redrawTokens
 */
internal class DummySpan private constructor() : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) {}
    override fun updateDrawState(tp: TextPaint) {}

    companion object {
        val INSTANCE = DummySpan()
    }
}