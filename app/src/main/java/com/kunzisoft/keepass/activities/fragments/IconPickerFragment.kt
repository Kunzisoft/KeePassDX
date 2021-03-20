package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.adapters.IconPickerPagerAdapter
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

class IconPickerFragment : StylishFragment() {

    private var iconPickerPagerAdapter: IconPickerPagerAdapter? = null
    private lateinit var viewPager: ViewPager2

    private val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    private var mDatabase: Database? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_icon_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mDatabase = Database.getInstance()

        viewPager = view.findViewById(R.id.icon_picker_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.icon_picker_tabs)
        iconPickerPagerAdapter = IconPickerPagerAdapter(this,
                if (mDatabase?.allowCustomIcons == true) 2 else 1)
        viewPager.adapter = iconPickerPagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.icon_section_custom)
                else -> getString(R.string.icon_section_standard)
            }
        }.attach()

        arguments?.apply {
            if (containsKey(ICON_TAB_ARG)) {
                viewPager.currentItem = getInt(ICON_TAB_ARG)
            }
            remove(ICON_TAB_ARG)
        }

        iconPickerViewModel.customIconAdded.observe(viewLifecycleOwner) { _ ->
            viewPager.currentItem = 1
        }
    }

    enum class IconTab {
        STANDARD, CUSTOM
    }

    companion object {

        private const val ICON_TAB_ARG = "ICON_TAB_ARG"

        fun getInstance(iconTab: IconTab): IconPickerFragment {
            val fragment = IconPickerFragment()
            fragment.arguments = Bundle().apply {
                putInt(ICON_TAB_ARG, iconTab.ordinal)
            }
            return fragment
        }
    }
}