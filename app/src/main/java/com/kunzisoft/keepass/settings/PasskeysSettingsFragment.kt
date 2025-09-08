/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preferencedialogfragment.PasskeysPrivilegedAppsPreferenceDialogFragmentCompat

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeysSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_passkeys, rootKey)
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        var dialogFragment: DialogFragment? = null

        when (preference.key) {
            getString(R.string.passkeys_privileged_apps_key) -> {
                dialogFragment = PasskeysPrivilegedAppsPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            else -> {}
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_PASSKEYS_PREF_FRAGMENT)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {

        private const val TAG_PASSKEYS_PREF_FRAGMENT = "TAG_PASSKEYS_PREF_FRAGMENT"
    }
}
