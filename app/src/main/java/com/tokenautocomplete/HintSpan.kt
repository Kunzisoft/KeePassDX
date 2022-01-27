package com.tokenautocomplete

import android.content.res.ColorStateList
import android.text.style.TextAppearanceSpan

/**
 * Subclass of TextAppearanceSpan just to work with how Spans get detected
 *
 * Created on 2/3/15.
 * @author mgod
 */
internal class HintSpan(
    family: String?,
    style: Int,
    size: Int,
    color: ColorStateList?,
    linkColor: ColorStateList?
) : TextAppearanceSpan(family, style, size, color, linkColor)