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
import com.kunzisoft.keepass.adapters.IconPickerPagerAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.viewmodels.IconPickerViewModel

class IconPickerFragment : DatabaseFragment() {

    private var iconPickerPagerAdapter: IconPickerPagerAdapter? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val iconPickerViewModel: IconPickerViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tabs_pagination, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.tabs_view_pager)
        tabLayout = view.findViewById(R.id.tabs_layout)
        resetAppTimeoutWhenViewFocusedOrChanged(view)

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

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        iconPickerPagerAdapter = IconPickerPagerAdapter(this,
            if (database?.allowCustomIcons == true) 2 else 1)
        viewPager.adapter = iconPickerPagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.icon_section_custom)
                else -> getString(R.string.icon_section_standard)
            }
        }.attach()
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