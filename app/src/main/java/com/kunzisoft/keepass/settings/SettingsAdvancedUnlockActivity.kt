package com.kunzisoft.keepass.settings

import android.os.Bundle
import androidx.fragment.app.Fragment


class SettingsAdvancedUnlockActivity : SettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mTimeoutEnable = false
    }

    override fun retrieveMainFragment(): Fragment {
        return NestedSettingsFragment.newInstance(NestedSettingsFragment.Screen.ADVANCED_UNLOCK)
    }
}