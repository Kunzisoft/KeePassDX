/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.IdRes
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import com.kunzisoft.keepass.R

class SectionView @JvmOverloads constructor(context: Context,
                                            attrs: AttributeSet? = null,
                                            defStyle: Int = R.attr.cardViewStyle)
    : CardView(context, attrs, defStyle) {

    private var containerSectionView = LinearLayout(context).apply {
        val padding = resources.getDimensionPixelSize(R.dimen.card_view_padding)
        layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        setPadding(padding)
        orientation = LinearLayout.VERTICAL
    }

    init {
        val marginHorizontal = resources.getDimensionPixelSize(R.dimen.card_view_margin_horizontal)
        val marginVertical = resources.getDimensionPixelSize(R.dimen.card_view_margin_vertical)
        layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT).also {
            it.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
        }
        visibility = View.GONE
        super.addView(containerSectionView)
    }

    override fun addView(child: View?) {
        visibility = View.VISIBLE
        containerSectionView.addView(child)
    }

    fun removeViewById(@IdRes viewId: Int, onFinish: ((View) ->Unit)? = null) {
        containerSectionView.findViewById<View?>(viewId)?.let { viewToRemove ->
            viewToRemove.collapse(true) {
                containerSectionView.removeView(viewToRemove)
                onFinish?.invoke(viewToRemove)
                // Hide section if needed
                try {
                    if (containerSectionView.childCount == 0) {
                        collapse(true)
                    }
                } catch (e: Exception) {
                    visibility = View.GONE
                }
            }
        }
    }

    override fun removeAllViews() {
        containerSectionView.removeAllViews()
        visibility = View.GONE
    }
}
