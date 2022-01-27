package com.tokenautocomplete

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import com.tokenautocomplete.TokenCompleteTextView.TokenImageSpan

internal object SpanUtils {
    @JvmStatic
    fun ellipsizeWithSpans(
        prefix: CharSequence?, countSpan: CountSpan?,
        tokenCount: Int, paint: TextPaint,
        originalText: CharSequence, maxWidth: Float
    ): Spanned? {
        var countWidth = 0f
        if (countSpan != null) {
            //Assume the largest possible number of items for measurement
            countSpan.setCount(tokenCount)
            countWidth = countSpan.getCountTextWidthForPaint(paint)
        }
        val ellipsizeCallback = EllipsizeCallback()
        val tempEllipsized = TextUtils.ellipsize(
            originalText, paint, maxWidth - countWidth,
            TextUtils.TruncateAt.END, false, ellipsizeCallback
        )
        val ellipsized = SpannableStringBuilder(tempEllipsized)
        if (tempEllipsized is Spanned) {
            TextUtils.copySpansFrom(
                tempEllipsized,
                0,
                tempEllipsized.length,
                Any::class.java,
                ellipsized,
                0
            )
        }
        if (prefix != null && prefix.length > ellipsizeCallback.start) {
            //We ellipsized part of the prefix, so put it back
            ellipsized.replace(0, ellipsizeCallback.start, prefix)
            ellipsizeCallback.end = ellipsizeCallback.end + prefix.length - ellipsizeCallback.start
            ellipsizeCallback.start = prefix.length
        }
        if (ellipsizeCallback.start != ellipsizeCallback.end) {
            if (countSpan != null) {
                val visibleCount =
                    ellipsized.getSpans(0, ellipsized.length, TokenImageSpan::class.java).size
                countSpan.setCount(tokenCount - visibleCount)
                ellipsized.replace(ellipsizeCallback.start, ellipsized.length, countSpan.countText)
                ellipsized.setSpan(
                    countSpan, ellipsizeCallback.start, ellipsized.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return ellipsized
        }
        //No ellipses necessary
        return null
    }

    private class EllipsizeCallback : TextUtils.EllipsizeCallback {
        var start = 0
        var end = 0
        override fun ellipsized(ellipsedStart: Int, ellipsedEnd: Int) {
            start = ellipsedStart
            end = ellipsedEnd
        }
    }
}