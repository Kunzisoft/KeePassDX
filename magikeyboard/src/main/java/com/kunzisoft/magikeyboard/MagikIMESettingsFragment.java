package com.kunzisoft.magikeyboard;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class MagikIMESettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.ime_preferences, rootKey);
    }
}
