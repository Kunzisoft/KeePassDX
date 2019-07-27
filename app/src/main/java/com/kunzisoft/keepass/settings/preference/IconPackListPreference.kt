package com.kunzisoft.keepass.settings.preference

import android.content.Context
import android.support.v7.preference.ListPreference
import android.util.AttributeSet
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.icons.IconPackChooser
import java.util.*

class IconPackListPreference @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = R.attr.dialogPreferenceStyle,
                                                       defStyleRes: Int = defStyleAttr)
    : ListPreference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        val entries = ArrayList<String>()
        val values = ArrayList<String>()
        for (iconPack in IconPackChooser.getIconPackList(context)) {
            if (iconPack.id != null) {
                entries.add(iconPack.name)
                values.add(iconPack.id!!)
            }
        }

        setEntries(entries.toTypedArray())
        entryValues = values.toTypedArray()
        IconPackChooser.getSelectedIconPack(context)?.let { selectedIconPack ->
            setDefaultValue(selectedIconPack.id)
        }
    }
}
