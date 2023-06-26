/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.settings.preferencedialogfragment.AutofillBlocklistAppIdPreferenceDialogFragmentCompat
import com.kunzisoft.keepass.settings.preferencedialogfragment.AutofillBlocklistWebDomainPreferenceDialogFragmentCompat

class AutofillSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_autofill, rootKey)

        val autofillInlineSuggestionsPreference: TwoStatePreference? = findPreference(getString(R.string.autofill_inline_suggestions_key))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            autofillInlineSuggestionsPreference?.isVisible = false
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null

        when (preference.key) {
            getString(R.string.autofill_application_id_blocklist_key) -> {
                dialogFragment = AutofillBlocklistAppIdPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.autofill_web_domain_blocklist_key) -> {
                dialogFragment = AutofillBlocklistWebDomainPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            else -> otherDialogFragment = true
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_AUTOFILL_PREF_FRAGMENT)
        }
        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    companion object {

        private const val TAG_AUTOFILL_PREF_FRAGMENT = "TAG_AUTOFILL_PREF_FRAGMENT"
    }
}
