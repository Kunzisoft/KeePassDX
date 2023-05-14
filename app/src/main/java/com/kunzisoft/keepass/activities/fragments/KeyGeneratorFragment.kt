/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.KeyGeneratorPagerAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.viewmodels.KeyGeneratorViewModel

class KeyGeneratorFragment : DatabaseFragment() {

    private var keyGeneratorPagerAdapter: KeyGeneratorPagerAdapter? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val mKeyGeneratorViewModel: KeyGeneratorViewModel by activityViewModels()

    private var mSelectedTab = KeyGeneratorTab.PASSWORD
    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback = object:
        ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            mSelectedTab = KeyGeneratorTab.getKeyGeneratorTabByPosition(position)
        }
    }

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
        viewPager.registerOnPageChangeCallback(mOnPageChangeCallback)

        resetAppTimeoutWhenViewFocusedOrChanged(view)

        arguments?.apply {
            if (containsKey(PASSWORD_TAB_ARG)) {
                viewPager.currentItem = getInt(PASSWORD_TAB_ARG)
            }
            remove(PASSWORD_TAB_ARG)
        }

        mKeyGeneratorViewModel.requireKeyGeneration.observe(viewLifecycleOwner) {
            when (mSelectedTab) {
                KeyGeneratorTab.PASSWORD -> {
                    mKeyGeneratorViewModel.requirePasswordGeneration()
                }
                KeyGeneratorTab.PASSPHRASE -> {
                    mKeyGeneratorViewModel.requirePassphraseGeneration()
                }
            }
        }

        mKeyGeneratorViewModel.keyGeneratedValidated.observe(viewLifecycleOwner) {
            when (mSelectedTab) {
                KeyGeneratorTab.PASSWORD -> {
                    mKeyGeneratorViewModel.validatePasswordGenerated()
                }
                KeyGeneratorTab.PASSPHRASE -> {
                    mKeyGeneratorViewModel.validatePassphraseGenerated()
                }
            }
        }
    }

    override fun onDestroyView() {
        viewPager.unregisterOnPageChangeCallback(mOnPageChangeCallback)
        super.onDestroyView()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
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