/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.settings

import android.content.Context
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.app.App

class MainPreferenceFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var mCallback: Callback? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is Callback) {
            mCallback = context
        } else {
            throw IllegalStateException("Owner must implement " + Callback::class.java.name)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // add listeners for non-default actions
        var preference = findPreference(getString(R.string.app_key))
        preference.onPreferenceClickListener = this

        preference = findPreference(getString(R.string.settings_form_filling_key))
        preference.onPreferenceClickListener = this

        preference = findPreference(getString(R.string.settings_appearance_key))
        preference.onPreferenceClickListener = this

        preference = findPreference(getString(R.string.db_key))
        preference.onPreferenceClickListener = this

        if (!App.currentDatabase.loaded) {
            preference.isEnabled = false
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        // here you should use the same keys as you used in the xml-file
        if (preference.key == getString(R.string.app_key)) {
            mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPLICATION)
        }

        if (preference.key == getString(R.string.settings_form_filling_key)) {
            mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.FORM_FILLING)
        }

        if (preference.key == getString(R.string.db_key)) {
            mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE)
        }

        if (preference.key == getString(R.string.settings_appearance_key)) {
            mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPEARANCE)
        }

        return false
    }

    interface Callback {
        fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen)
    }
}