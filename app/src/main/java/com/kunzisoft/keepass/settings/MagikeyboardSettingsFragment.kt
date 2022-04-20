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

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat

class MagikeyboardSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_keyboard, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.keyboard_entry_timeout_key) -> {
                dialogFragment = DurationDialogFragmentCompat.newInstance(preference.key)
            }
            else -> otherDialogFragment = true
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_PREF_FRAGMENT)
        }
        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {
        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"
    }
}
