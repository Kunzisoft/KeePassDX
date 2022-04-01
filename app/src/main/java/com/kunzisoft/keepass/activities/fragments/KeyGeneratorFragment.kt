package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.KeyGeneratorPagerAdapter
import com.kunzisoft.keepass.database.element.Database

class KeyGeneratorFragment : DatabaseFragment() {

    private var keyGeneratorPagerAdapter: KeyGeneratorPagerAdapter? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tabs_pagination, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keyGeneratorPagerAdapter = KeyGeneratorPagerAdapter(this, )
        viewPager = view.findViewById(R.id.tabs_view_pager)
        tabLayout = view.findViewById(R.id.tabs_layout)
        viewPager.adapter = keyGeneratorPagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(KeyGeneratorTab.getKeyGeneratorTabByPosition(position).stringId)
        }.attach()

        resetAppTimeoutWhenViewFocusedOrChanged(view)

        arguments?.apply {
            if (containsKey(PASSWORD_TAB_ARG)) {
                viewPager.currentItem = getInt(PASSWORD_TAB_ARG)
            }
            remove(PASSWORD_TAB_ARG)
        }
    }

    override fun onDatabaseRetrieved(database: Database?) {
        // Nothing here
    }

    enum class KeyGeneratorTab(@StringRes val stringId: Int) {
        PASSWORD(R.string.password), PASSPHRASE(R.string.passphrase);

        companion object {
            fun getKeyGeneratorTabByPosition(position: Int): KeyGeneratorTab {
                return when (position) {
                    0 -> PASSWORD
                    else -> PASSPHRASE
                }
            }
        }
    }

    companion object {

        private const val PASSWORD_TAB_ARG = "PASSWORD_TAB_ARG"

        fun getInstance(keyGeneratorTab: KeyGeneratorTab): KeyGeneratorFragment {
            val fragment = KeyGeneratorFragment()
            fragment.arguments = Bundle().apply {
                putInt(PASSWORD_TAB_ARG, keyGeneratorTab.ordinal)
            }
            return fragment
        }
    }
}