package com.kunzisoft.keepass.activities.fragments

import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage

/**
 * Content fragments
 */

class IconCustomFragment : IconFragment() {

    override fun retrieveMainLayoutId(): Int {
        return R.layout.fragment_icon_custom_picker
    }

    override fun defineIconList(database: Database): List<IconImage> {
        return database.iconPool.getCustomIconList()
    }
}