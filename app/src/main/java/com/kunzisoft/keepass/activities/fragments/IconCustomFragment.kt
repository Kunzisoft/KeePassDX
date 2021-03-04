package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom

/**
 * Content fragments
 */

class IconCustomFragment : IconFragment<IconImageCustom>() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_custom_picker
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