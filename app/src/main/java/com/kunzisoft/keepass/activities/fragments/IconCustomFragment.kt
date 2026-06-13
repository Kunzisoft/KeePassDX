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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import kotlinx.coroutines.launch


class IconCustomFragment : IconFragment<IconImageCustom>() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_grid
    }

    override fun defineIconList(database: ContextualDatabase?) {
        database?.doForEachCustomIcons { customIcon, _ ->
            iconPickerAdapter.addIcon(customIcon, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    iconPickerViewModel.customIconsSelected.collect { customIconsSelected ->
                        if (customIconsSelected.isEmpty()) {
                            iconActionSelectionMode = false
                            iconPickerAdapter.deselectAllIcons()
                        } else {
                            iconActionSelectionMode = true
                            iconPickerAdapter.updateIconSelectedState(customIconsSelected)
                        }
                    }
                }
                launch {
                    iconPickerViewModel.customIconAdded.collect { iconCustomAdded ->
                        if (!iconCustomAdded.error) {
                            iconCustomAdded.iconCustom?.let { icon ->
                                iconPickerAdapter.addIcon(icon)
                                iconCustomAdded.iconCustom = null
                                try {
                                    iconsGridView.smoothScrollToPosition(iconPickerAdapter.lastPosition)
                                } catch (ignore: Exception) {
                                }
                            }
                        }
                    }
                }
                launch {
                    iconPickerViewModel.customIconRemoved.collect { iconCustomRemoved ->
                        if (!iconCustomRemoved.error) {
                            iconCustomRemoved.iconCustom?.let { icon ->
                                iconPickerAdapter.removeIcon(icon)
                                iconCustomRemoved.iconCustom = null
                            }
                        }
                    }
                }
                launch {
                    iconPickerViewModel.customIconUpdated.collect { iconCustomUpdated ->
                        if (!iconCustomUpdated.error) {
                            iconCustomUpdated.iconCustom?.let { icon ->
                                iconPickerAdapter.updateIcon(icon)
                                iconCustomUpdated.iconCustom = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onIconClickListener(icon: IconImageCustom) {
        if (iconActionSelectionMode) {
            // Same long click behavior after each single click
            onIconLongClickListener(icon)
        } else {
            iconPickerViewModel.pickCustomIcon(icon)
        }
    }

    override fun onIconLongClickListener(icon: IconImageCustom) {
        // Select or deselect item if already selected
        icon.selected = !icon.selected
        iconPickerAdapter.updateIcon(icon)
        iconActionSelectionMode = iconPickerAdapter.containsAnySelectedIcon()
        iconPickerViewModel.selectCustomIcons(iconPickerAdapter.getSelectedIcons())
    }
}