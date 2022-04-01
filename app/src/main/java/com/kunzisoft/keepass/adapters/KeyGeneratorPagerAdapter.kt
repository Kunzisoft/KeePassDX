package com.kunzisoft.keepass.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kunzisoft.keepass.activities.fragments.PassphraseGeneratorFragment
import com.kunzisoft.keepass.activities.fragments.PasswordGeneratorFragment
import com.kunzisoft.keepass.activities.fragments.KeyGeneratorFragment

class KeyGeneratorPagerAdapter(fragment: Fragment)
    : FragmentStateAdapter(fragment) {

    private val passwordGeneratorFragment = PasswordGeneratorFragment()
    private val passphraseGeneratorFragment = PassphraseGeneratorFragment()

    override fun getItemCount(): Int {
        return KeyGeneratorFragment.KeyGeneratorTab.values().size
    }

    override fun createFragment(position: Int): Fragment {
        return when (KeyGeneratorFragment.KeyGeneratorTab.getKeyGeneratorTabByPosition(position)) {
            KeyGeneratorFragment.KeyGeneratorTab.PASSWORD -> passwordGeneratorFragment
            KeyGeneratorFragment.KeyGeneratorTab.PASSPHRASE -> passphraseGeneratorFragment
        }
    }
}