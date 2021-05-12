/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.kunzisoft.keepass.R

class SectionView @JvmOverloads constructor(context: Context,
                                            attrs: AttributeSet? = null,
                                            defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private val containerSectionView: ViewGroup

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_section, this)

        containerSectionView = findViewById(R.id.section_container)
        containerSectionView.visibility = View.GONE
    }

    fun addAttributeView(view: View?) {
        containerSectionView.postDelayed({
            containerSectionView.apply {
                alpha = 0f
                visibility = View.VISIBLE
                addView(view)
                animate()
                        .alpha(1f)
                        .setDuration(200)
                        .setListener(null)
            }
        }, 200)
    }

    fun clear() {
        containerSectionView.visibility = View.GONE
        containerSectionView.removeAllViews()
    }
}
