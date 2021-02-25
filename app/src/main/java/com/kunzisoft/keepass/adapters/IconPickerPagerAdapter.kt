package com.kunzisoft.keepass.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.activities.fragments.IconCustomFragment
import com.kunzisoft.keepass.activities.fragments.IconStandardFragment

class IconPickerPagerAdapter(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_SET_USER_VISIBLE_HINT) {

    var iconSelected: IconPickerDialogFragment.IconPickerListener? = null
    private val iconStandardFragment = IconStandardFragment()
    private val iconCustomFragment = IconCustomFragment()

    override fun getCount(): Int  = 2

    override fun getItem(i: Int): Fragment {
        return when (i) {
            1 -> iconCustomFragment
            else -> iconStandardFragment
        }.apply {
            iconListener = iconSelected
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            1 -> "Custom" //context.getString(R.string.iconStandard)
            else -> "Standard" //context.getString(R.string.iconStandard)
        }
    }
}