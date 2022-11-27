package com.kunzisoft.keepass.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kunzisoft.keepass.activities.fragments.IconCustomFragment
import com.kunzisoft.keepass.activities.fragments.IconLoaderFragment
import com.kunzisoft.keepass.activities.fragments.IconStandardFragment

class IconPickerPagerAdapter(fragment: Fragment, val size: Int)
    : FragmentStateAdapter(fragment) {

    private val iconStandardFragment = IconStandardFragment()
    private val iconCustomFragment = IconCustomFragment()
    private val iconLoaderFragment = IconLoaderFragment()

    override fun getItemCount(): Int = size

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> iconStandardFragment
        1 -> iconCustomFragment
        2 -> iconLoaderFragment
        else -> error("Invalid position '$position'.")
    }
}