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
package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.adapters.IconPickerAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

abstract class IconFragment<T: IconImageDraw> : StylishFragment(),
        IconPickerAdapter.IconPickerListener<T> {

    protected lateinit var iconsGridView: RecyclerView
    protected lateinit var iconPickerAdapter: IconPickerAdapter<T>
    protected var iconActionSelectionMode = false

    protected val database = Database.getInstance()

    protected val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    abstract fun retrieveMainLayoutId(): Int

    abstract fun defineIconList(database: Database): List<T>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Retrieve the textColor to tint the icon
        val ta = contextThemed?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        val tintColor = ta?.getColor(0, Color.BLACK) ?: Color.BLACK
        ta?.recycle()

        iconPickerAdapter = IconPickerAdapter<T>(context, tintColor).apply {
            iconDrawableFactory = database.iconDrawableFactory
        }

        iconPickerAdapter.setList(defineIconList(database))
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val root = inflater.inflate(retrieveMainLayoutId(), container, false)
        iconsGridView = root.findViewById(R.id.icons_grid_view)
        iconsGridView.adapter = iconPickerAdapter
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iconPickerAdapter.iconPickerListener = this
    }

    fun onIconDeleteClicked() {
        iconActionSelectionMode = false
    }
}