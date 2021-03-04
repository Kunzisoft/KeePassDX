package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.View
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageStandard

/**
 * Content fragments
 */

class IconStandardFragment : IconFragment<IconImageStandard>() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_grid
    }

    override fun defineIconList(database: Database): List<IconImageStandard> {
        return database.iconsManager.getStandardIconList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iconAdapter.iconPickerListener = object : IconAdapter.IconPickerListener<IconImageStandard> {
            override fun iconPicked(icon: IconImageStandard) {
                iconPickerViewModel.selectIconStandard(icon)
            }
        }
    }
}