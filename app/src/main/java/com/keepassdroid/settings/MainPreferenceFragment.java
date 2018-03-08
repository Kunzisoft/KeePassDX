package com.keepassdroid.settings;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.keepassdroid.app.App;
import com.keepassdroid.database.Database;
import com.kunzisoft.keepass.R;

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

        preference = findPreference(getString(R.string.db_key));
        preference.setOnPreferenceClickListener(this);
        Database db = App.getDB();
        if (!(db.Loaded())) {
            preference.setEnabled(false);
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // Try if the preference is one of our custom Preferences
        if (preference instanceof RoundsPreference) {
            DialogFragment dialogFragment = RoundsFixPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        }
        // Could not be handled here. Try with the super method.
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // here you should use the same keys as you used in the xml-file
        if (preference.getKey().equals(getString(R.string.app_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.NESTED_SCREEN_APP_KEY);
        }

        if (preference.getKey().equals(getString(R.string.settings_form_filling_key))) {
            mCallback.onNestedPreferenceSelected(NestedSettingsFragment.NESTED_SCREEN_FORM_FILLING_KEY);
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