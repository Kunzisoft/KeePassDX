/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.autofill.AutofillManager;

import com.kunzisoft.keepass.R;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private static final int REQUEST_CODE_SET_DEFAULT = 1;
    private AutofillManager mAutofillManager;
    private SwitchPreference enablePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.autofill_preferences, rootKey);
        mAutofillManager = getActivity().getSystemService(AutofillManager.class);

        // add listeners for non-default actions
        enablePreference = (SwitchPreference) findPreference(getString(R.string.settings_autofill_enable_key));
    }

    @Override
    public void onResume() {
        super.onResume();

        enablePreference.setOnPreferenceClickListener(null);
        if (mAutofillManager != null && mAutofillManager.hasEnabledAutofillServices())
            enablePreference.setChecked(mAutofillManager.hasEnabledAutofillServices());
        enablePreference.setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.settings_autofill_enable_key))) {
            setService(((SwitchPreference) preference).isChecked());
        }

        return false;
    }

    private void setService(boolean enableService) {
        if (enableService) {
            startEnableService();
        } else {
            disableService();
        }
    }

    private void disableService() {
        if (mAutofillManager != null && mAutofillManager.hasEnabledAutofillServices()) {
            mAutofillManager.disableAutofillServices();
        } else {
            Log.d(getClass().getName(), "Sample service already disabled.");
        }
    }

    private void startEnableService() {
        if (mAutofillManager != null && !mAutofillManager.hasEnabledAutofillServices()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
            intent.setData(Uri.parse("package:com.example.android.autofill.service"));
            Log.d(getClass().getName(), "enableService(): intent="+ intent);
            startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT);
        } else {
            Log.d(getClass().getName(), "Sample service already enabled.");
        }
    }
}