package com.kunzisoft.keepass.settings;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.kunzisoft.keepass.R;

public class MagikIMESettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.keyboard_preferences, rootKey);
    }
}
