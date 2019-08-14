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
import com.kunzisoft.keepass.activities.dialogs.AssignMasterKeyDialogFragment
import com.kunzisoft.keepass.database.element.Database

class MainPreferenceFragment : PreferenceFragmentCompat() {

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
        findPreference(getString(R.string.app_key)).apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPLICATION)
                false
            }
        }

        findPreference(getString(R.string.settings_form_filling_key)).apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.FORM_FILLING)
                false
            }
        }

        findPreference(getString(R.string.settings_appearance_key)).apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPEARANCE)
                false
            }
        }

        findPreference(getString(R.string.database_main_menu_key)).apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                mCallback?.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE)
                false
            }
            if (!Database.getInstance().loaded) {
                isEnabled = false
            }
        }

        findPreference(getString(R.string.database_change_master_key_key)).apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                AssignMasterKeyDialogFragment().show(fragmentManager, "passwordDialog")
                false
            }
        }
    }

    interface Callback {
        fun onNestedPreferenceSelected(key: NestedSettingsFragment.Screen)
    }
}