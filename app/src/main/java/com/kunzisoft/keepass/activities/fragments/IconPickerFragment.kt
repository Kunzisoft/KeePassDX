package com.kunzisoft.keepass.activities.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.IconAdapter
import com.kunzisoft.keepass.adapters.IconPickerPagerAdapter

class IconPickerFragment : Fragment() {

    private var mListener: IconAdapter.IconPickerListener? = null

    private var iconPickerPagerAdapter: IconPickerPagerAdapter? = null
    private lateinit var viewPager: ViewPager2

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as IconAdapter.IconPickerListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + IconAdapter.IconPickerListener::class.java.name)
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

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

        // TODO fix listener
        viewPager.adapter = iconPickerPagerAdapter!!.apply {
            setListeners(mListener, mListener)
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> "Custom"
                else -> "Standard"
            }
        }.attach()
    }
}