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

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.database.Database;
import com.keepassdroid.database.PwDatabase;
import com.keepassdroid.dialogs.StorageAccessFrameworkDialog;
import com.keepassdroid.dialogs.UnavailableFeatureDialogFragment;
import com.keepassdroid.fingerprint.FingerPrintHelper;
import com.keepassdroid.stylish.Stylish;
import com.kunzisoft.keepass.R;

public class NestedSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    public static final int NESTED_SCREEN_APP_KEY = 1;
    public static final int NESTED_SCREEN_FORM_FILLING_KEY = 2;
    public static final int NESTED_SCREEN_DB_KEY = 3;

    private static final String TAG_KEY = "NESTED_KEY";

    private static final int REQUEST_CODE_AUTOFILL = 5201;

    public static NestedSettingsFragment newInstance(int key) {
        NestedSettingsFragment fragment = new NestedSettingsFragment();
        // supply arguments to bundle.
        Bundle args = new Bundle();
        args.putInt(TAG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SwitchPreference autoFillEnablePreference =
                    (SwitchPreference) findPreference(getString(R.string.settings_autofill_enable_key));
            if (autoFillEnablePreference != null) {
                AutofillManager autofillManager = getActivity().getSystemService(AutofillManager.class);
                if (autofillManager != null && autofillManager.hasEnabledAutofillServices())
                    autoFillEnablePreference.setChecked(true);
                else
                    autoFillEnablePreference.setChecked(false);
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        int key = getArguments().getInt(TAG_KEY);
        // Load the preferences from an XML resource
        switch (key) {
            case NESTED_SCREEN_APP_KEY:
                setPreferencesFromResource(R.xml.app_preferences, rootKey);

                Preference keyFile = findPreference(getString(R.string.keyfile_key));
                keyFile.setOnPreferenceChangeListener((preference, newValue) -> {
                    Boolean value = (Boolean) newValue;
                    if (!value) {
                        App.getFileHistory().deleteAllKeys();
                    }
                    return true;
                });

                Preference recentHistory = findPreference(getString(R.string.recentfile_key));
                recentHistory.setOnPreferenceChangeListener((preference, newValue) -> {
                    Boolean value = (Boolean) newValue;
                    if (value == null) {
                        value = true;
                    }
                    if (!value) {
                        App.getFileHistory().deleteAll();
                    }
                    return true;
                });

                SwitchPreference storageAccessFramework = (SwitchPreference) findPreference(getString(R.string.saf_key));
                storageAccessFramework.setOnPreferenceChangeListener((preference, newValue) -> {
                    Boolean value = (Boolean) newValue;
                    if (!value && getContext() != null) {
                        StorageAccessFrameworkDialog safDialog = new StorageAccessFrameworkDialog(getContext());
                        safDialog.setButton(AlertDialog.BUTTON1, getText(android.R.string.ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                });
                        safDialog.setButton(AlertDialog.BUTTON2, getText(android.R.string.cancel),
                                (dialog, which) -> {
                                    storageAccessFramework.setChecked(true);
                                    dialog.dismiss();
                                });
                        safDialog.show();
                    }
                    return true;
                });

                Preference stylePreference = findPreference(getString(R.string.setting_style_key));
                stylePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String styleString = (String) newValue;
                    Stylish.assignStyle(getActivity(), styleString);
                    getActivity().recreate();
                    return true;
                });

                SwitchPreference fingerprintEnablePreference =
                        (SwitchPreference) findPreference(getString(R.string.fingerprint_enable_key));
                // < M solve verifyError exception
                boolean fingerprintSupported = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    fingerprintSupported = FingerPrintHelper.isFingerprintSupported(
                            FingerprintManagerCompat.from(getContext()));
                if (!fingerprintSupported) {
                    // False if under Marshmallow
                    fingerprintEnablePreference.setChecked(false);
                    fingerprintEnablePreference.setOnPreferenceClickListener(preference -> {
                        FragmentManager fragmentManager = getFragmentManager();
                        assert fragmentManager != null;
                        ((SwitchPreference) preference).setChecked(false);
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                                .show(getFragmentManager(), "unavailableFeatureDialog");
                        return false;
                    });
                }

                Preference deleteKeysFingerprints = findPreference(getString(R.string.fingerprint_delete_all_key));
                if (!fingerprintSupported) {
                    deleteKeysFingerprints.setEnabled(false);
                } else {
                    deleteKeysFingerprints.setOnPreferenceClickListener(preference -> {
                        new AlertDialog.Builder(getContext())
                                .setMessage(getResources().getString(R.string.fingerprint_delete_all_warning))
                                .setIcon(getResources().getDrawable(
                                        android.R.drawable.ic_dialog_alert))
                                .setPositiveButton(
                                        getResources().getString(android.R.string.yes),
                                        new DialogInterface.OnClickListener() {
                                            @RequiresApi(api = Build.VERSION_CODES.M)
                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                FingerPrintHelper.deleteEntryKeyInKeystoreForFingerprints(
                                                        getContext(),
                                                        new FingerPrintHelper.FingerPrintErrorCallback() {
                                                            @Override
                                                            public void onInvalidKeyException(Exception e) {}

                                                            @Override
                                                            public void onFingerPrintException(Exception e) {
                                                                Toast.makeText(getContext(),
                                                                        getString(R.string.fingerprint_error, e.getLocalizedMessage()),
                                                                        Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                PreferencesUtil.deleteAllValuesFromNoBackupPreferences(getContext());
                                            }
                                        })
                                .setNegativeButton(
                                        getResources().getString(android.R.string.no),
                                        (dialog, which) -> {
                                        }).show();
                        return false;
                    });
                }
                break;

            case NESTED_SCREEN_FORM_FILLING_KEY:
                setPreferencesFromResource(R.xml.form_filling_preferences, rootKey);

                SwitchPreference autoFillEnablePreference =
                        (SwitchPreference) findPreference(getString(R.string.settings_autofill_enable_key));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    assert getActivity() != null;
                    AutofillManager autofillManager = getActivity().getSystemService(AutofillManager.class);
                    if (autofillManager != null && autofillManager.hasEnabledAutofillServices())
                        autoFillEnablePreference.setChecked(autofillManager.hasEnabledAutofillServices());
                    autoFillEnablePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (((SwitchPreference) preference).isChecked()) {
                                try {
                                    startEnableService();
                                } catch (ActivityNotFoundException e) {
                                    String error = getString(R.string.error_autofill_enable_service);
                                    ((SwitchPreference) preference).setChecked(false);
                                    Log.d(getClass().getName(), error, e);
                                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                disableService();
                            }
                            return false;
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        private void disableService() {
                            if (autofillManager != null && autofillManager.hasEnabledAutofillServices()) {
                                autofillManager.disableAutofillServices();
                            } else {
                                Log.d(getClass().getName(), "Sample service already disabled.");
                            }
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        private void startEnableService() throws ActivityNotFoundException{
                            if (autofillManager != null && !autofillManager.hasEnabledAutofillServices()) {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                                intent.setData(Uri.parse("package:com.example.android.autofill.service"));
                                Log.d(getClass().getName(), "enableService(): intent=" + intent);
                                startActivityForResult(intent, REQUEST_CODE_AUTOFILL);
                            } else {
                                Log.d(getClass().getName(), "Sample service already enabled.");
                            }
                        }
                    });
                } else {
                    autoFillEnablePreference.setOnPreferenceClickListener(preference -> {
                        ((SwitchPreference) preference).setChecked(false);
                        FragmentManager fragmentManager = getFragmentManager();
                        assert fragmentManager != null;
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.O)
                                .show(fragmentManager, "unavailableFeatureDialog");
                        return false;
                    });
                }
                break;

            case NESTED_SCREEN_DB_KEY:
                setPreferencesFromResource(R.xml.db_preferences, rootKey);

                Database db = App.getDB();
                if (db.getLoaded()) {

                    PwDatabase pwDatabase = db.getPwDatabase(); // Transit methods in db

                    // Encryption Algorithme
                    Preference algorithmPref = findPreference(getString(R.string.encryption_algorithm_key));
                    algorithmPref.setSummary(pwDatabase
                            .getEncryptionAlgorithm().getName(getResources()));

                    // Key derivation function
                    Preference kdfPref = findPreference(getString(R.string.key_derivation_function_key));
                    kdfPref.setSummary(pwDatabase
                            .getKeyDerivationName());

                    // Round encryption
                    Preference roundPref = findPreference(getString(R.string.transform_rounds_key));
                    roundPref.setSummary(Long.toString(pwDatabase.getNumberKeyEncryptionRounds()));
                    roundPref.setEnabled(false); //TODO refactor round pref
                    roundPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        preference.setSummary(Long.toString(pwDatabase.getNumberKeyEncryptionRounds()));
                        return true;
                    });

                    if (db.isRecycleBinAvailabledAndEnabled()) {
                        SwitchPreference recycleBinPref = (SwitchPreference) findPreference(getString(R.string.recycle_bin_key));
                        // TODO Recycle
                        //recycleBinPref.setEnabled(true);
                        recycleBinPref.setChecked(db.isRecycleBinAvailabledAndEnabled());
                    }

                } else {
                    Log.e(getClass().getName(), "Database isn't ready");
                }

                break;

            default:
                break;
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // Try if the preference is one of our custom Preferences
        if (preference instanceof RoundsPreference) {
            assert getFragmentManager() != null;
            DialogFragment dialogFragment = RoundsPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        }
        // Could not be handled here. Try with the super method.
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    public static String retrieveTitle(Resources resources, int key) {
        switch (key) {
            case NESTED_SCREEN_APP_KEY:
                return resources.getString(R.string.menu_app_settings);
            case NESTED_SCREEN_FORM_FILLING_KEY:
                return resources.getString(R.string.menu_form_filling_settings);
            case NESTED_SCREEN_DB_KEY:
                return resources.getString(R.string.menu_db_settings);
            default:
                return resources.getString(R.string.settings);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO encapsulate

        return false;
    }
}
