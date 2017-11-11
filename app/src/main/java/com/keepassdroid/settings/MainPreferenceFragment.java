package com.keepassdroid.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.android.keepass.R;
import com.keepassdroid.Database;
import com.keepassdroid.app.App;

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

        preference = findPreference(getString(R.string.db_key));
        Database db = App.getDB();
        if (!(db.Loaded() && db.pm.appSettingsEnabled())) {
            Preference dbSettings = findPreference(getString(R.string.db_key));
            dbSettings.setEnabled(false);
        } else {
            preference.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // here you should use the same keys as you used in the xml-file
        if (preference.getKey().equals(getString(R.string.app_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.NESTED_SCREEN_APP_KEY);
        }

        if (preference.getKey().equals(getString(R.string.db_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.NESTED_SCREEN_DB_KEY);
        }

        return false;
    }

    public interface Callback {
        void onNestedPreferenceSelected(int key);
    }
}