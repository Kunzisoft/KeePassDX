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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom


class IconCustomFragment : IconFragment<IconImageCustom>() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_grid
    }

    override fun defineIconList(database: Database): List<IconImageCustom> {
        return database.iconsManager.getCustomIconList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iconPickerViewModel.iconCustomAdded.observe(viewLifecycleOwner) { iconCustom ->
            if (!iconCustom.error) {
                iconAdapter.addIcon(iconCustom.iconCustom)
                iconsGridView.smoothScrollToPosition(iconAdapter.lastPosition)
            }
        }

        iconAdapter.iconPickerListener = object : IconAdapter.IconPickerListener<IconImageCustom> {
            override fun iconPicked(icon: IconImageCustom) {
                iconPickerViewModel.selectIconCustom(icon)
            }
        }
    }
}