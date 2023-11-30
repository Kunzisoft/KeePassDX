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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import com.kunzisoft.keepass.R

class SectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val containerSectionView: LinearLayout
    private val sectionTitleView: TextView

    init {
        inflate(context, R.layout.layout_section_view, this)
        containerSectionView = findViewById(R.id.container_view)
        sectionTitleView = findViewById(R.id.section_title_view)
    }

    override fun addView(child: View?) {
        visibility = View.VISIBLE
        containerSectionView.addView(child)
    }

    fun setSectionTitle(title: String?) {
        if (title == null || title?.isEmpty() == true) {
            sectionTitleView.visibility = View.GONE
        } else {
            sectionTitleView.visibility = View.VISIBLE
            sectionTitleView.text = title
        }
    }

    fun removeViewById(@IdRes viewId: Int, onFinish: ((View) -> Unit)? = null) {
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
