/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.settings

import android.content.res.Resources
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.UnderDevelopmentFeatureDialogFragment

abstract class NestedSettingsFragment : PreferenceFragmentCompat() {

    enum class Screen {
        APPLICATION, FORM_FILLING, ADVANCED_UNLOCK, APPEARANCE, DATABASE, DATABASE_SECURITY, DATABASE_MASTER_KEY
    }

    fun getScreen(): Screen {
        return Screen.values()[requireArguments().getInt(TAG_KEY)]
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        onCreateScreenPreference(
                getScreen(),
                savedInstanceState,
                rootKey)
    }

    abstract fun onCreateScreenPreference(screen: Screen, savedInstanceState: Bundle?, rootKey: String?)

    protected fun preferenceInDevelopment(preferenceInDev: Preference) {
        preferenceInDev.setOnPreferenceClickListener { preference ->
            try { // don't check if we can
                (preference as TwoStatePreference).isChecked = false
            } catch (ignored: Exception) {
            }
            UnderDevelopmentFeatureDialogFragment().show(parentFragmentManager, "underDevFeatureDialog")
            false
        }
    }

    companion object {

        private const val TAG_KEY = "NESTED_KEY"

        fun newInstance(key: Screen)
                : NestedSettingsFragment {
            val fragment: NestedSettingsFragment = when (key) {
                Screen.APPLICATION,
                Screen.FORM_FILLING,
                Screen.ADVANCED_UNLOCK,
                Screen.APPEARANCE -> NestedAppSettingsFragment()
                Screen.DATABASE,
                Screen.DATABASE_SECURITY,
                Screen.DATABASE_MASTER_KEY -> NestedDatabaseSettingsFragment()
            }
            // supply arguments to bundle.
            val args = Bundle()
            args.putInt(TAG_KEY, key.ordinal)
            fragment.arguments = args
            return fragment
        }

        fun retrieveTitle(resources: Resources, key: Screen): String {
            return when (key) {
                Screen.APPLICATION -> resources.getString(R.string.menu_app_settings)
                Screen.FORM_FILLING -> resources.getString(R.string.menu_form_filling_settings)
                Screen.ADVANCED_UNLOCK -> resources.getString(R.string.menu_advanced_unlock_settings)
                Screen.APPEARANCE -> resources.getString(R.string.menu_appearance_settings)
                Screen.DATABASE -> resources.getString(R.string.menu_database_settings)
                Screen.DATABASE_SECURITY -> resources.getString(R.string.menu_security_settings)
                Screen.DATABASE_MASTER_KEY -> resources.getString(R.string.menu_master_key_settings)
            }
        }
    }
}
