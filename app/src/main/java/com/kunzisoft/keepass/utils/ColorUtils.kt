package com.kunzisoft.keepass.utils

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.kunzisoft.keepass.R

object ColorUtils {
    @ColorInt
    fun getColorFromAttr(context: Context, @AttrRes attr: Int, @ColorInt defaultColor: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, defaultColor)
        ta.recycle()
        return color
    }
}