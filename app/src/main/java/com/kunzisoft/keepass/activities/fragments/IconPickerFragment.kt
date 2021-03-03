package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconPickerPagerAdapter
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

class IconPickerFragment : Fragment() {

    private var iconPickerPagerAdapter: IconPickerPagerAdapter? = null
    private lateinit var viewPager: ViewPager2

    private val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_icon_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.icon_picker_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.icon_picker_tabs)
        iconPickerPagerAdapter = IconPickerPagerAdapter(this)
        viewPager.adapter = iconPickerPagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.icon_section_custom)
                else -> getString(R.string.icon_section_standard)
            }
        }.attach()

        iconPickerViewModel.iconCustomAdded.observe(viewLifecycleOwner) { _ ->
            viewPager.currentItem = 1
        }
    }
}