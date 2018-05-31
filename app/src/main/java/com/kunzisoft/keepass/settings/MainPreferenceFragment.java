/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;

public class MainPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private Callback mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Callback) {
            mCallback = (Callback) context;
        } else {
            throw new IllegalStateException("Owner must implement " + Callback.class.getName());
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // add listeners for non-default actions
        Preference preference = findPreference(getString(R.string.app_key));
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(getString(R.string.settings_form_filling_key));
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(getString(R.string.settings_appearance_key));
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(getString(R.string.db_key));
        preference.setOnPreferenceClickListener(this);
        Database db = App.getDB();
        if (!(db.getLoaded())) {
            preference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // here you should use the same keys as you used in the xml-file
        if (preference.getKey().equals(getString(R.string.app_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPLICATION);
        }

        if (preference.getKey().equals(getString(R.string.settings_form_filling_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.Screen.FORM_FILLING);
        }

        if (preference.getKey().equals(getString(R.string.db_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.Screen.DATABASE);
        }

        if (preference.getKey().equals(getString(R.string.settings_appearance_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.Screen.APPEARANCE);
        }

        return false;
    }

    public interface Callback {
        void onNestedPreferenceSelected(NestedSettingsFragment.Screen key);
    }
}