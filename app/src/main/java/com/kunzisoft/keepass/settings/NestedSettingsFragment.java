/*
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
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

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.autofill.AutofillManager;
import android.widget.Toast;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.activities.ReadOnlyHelper;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.dialogs.ProFeatureDialogFragment;
import com.kunzisoft.keepass.dialogs.UnavailableFeatureDialogFragment;
import com.kunzisoft.keepass.dialogs.UnderDevelopmentFeatureDialogFragment;
import com.kunzisoft.keepass.fingerprint.FingerPrintHelper;
import com.kunzisoft.keepass.icons.IconPackChooser;
import com.kunzisoft.keepass.dialogs.KeyboardExplanationDialogFragment;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseDescriptionPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseKeyDerivationPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseNamePreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.MemoryUsagePreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.ParallelismPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.RoundsPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.stylish.Stylish;

public class NestedSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    public enum Screen {
        APPLICATION, FORM_FILLING, DATABASE, APPEARANCE
    }

    private static final String TAG_KEY = "NESTED_KEY";

    private static final int REQUEST_CODE_AUTOFILL = 5201;

    private Database database;
    private boolean databaseReadOnly;

    private int count = 0;

    private Preference roundPref;
    private Preference memoryPref;
    private Preference parallelismPref;

    public static NestedSettingsFragment newInstance(Screen key) {
        return newInstance(key, ReadOnlyHelper.READ_ONLY_DEFAULT);
    }

    public static NestedSettingsFragment newInstance(Screen key, boolean databaseReadOnly) {
        NestedSettingsFragment fragment = new NestedSettingsFragment();
        // supply arguments to bundle.
        Bundle args = new Bundle();
        args.putInt(TAG_KEY, key.ordinal());
        ReadOnlyHelper.putReadOnlyInBundle(args, databaseReadOnly);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        int key = 0;
        if (getArguments() != null)
            key = getArguments().getInt(TAG_KEY);

        database = App.getDB();
        databaseReadOnly = ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, getArguments());
        databaseReadOnly = database.isReadOnly() || databaseReadOnly;

        // Load the preferences from an XML resource
        switch (Screen.values()[key]) {
            case APPLICATION:
                setPreferencesFromResource(R.xml.application_preferences, rootKey);

                allowCopyPassword();

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
                        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                                .setMessage(getString(R.string.warning_disabling_storage_access_framework)).create();
                        alertDialog.setButton(AlertDialog.BUTTON1, getText(android.R.string.ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                });
                        alertDialog.setButton(AlertDialog.BUTTON2, getText(android.R.string.cancel),
                                (dialog, which) -> {
                                    storageAccessFramework.setChecked(true);
                                    dialog.dismiss();
                                });
                        alertDialog.show();
                    }
                    return true;
                });

                SwitchPreference fingerprintEnablePreference =
                        (SwitchPreference) findPreference(getString(R.string.fingerprint_enable_key));
                // < M solve verifyError exception
                boolean fingerprintSupported = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && getActivity() != null)
                    fingerprintSupported = FingerPrintHelper.isFingerprintSupported(
                            getActivity().getSystemService(FingerprintManager.class));
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

            case FORM_FILLING:
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

                Preference keyboardPreference = findPreference(getString(R.string.magic_keyboard_key));
                keyboardPreference.setOnPreferenceClickListener(preference -> {
                    if (getFragmentManager() != null) {
                        KeyboardExplanationDialogFragment keyboardDialog = new KeyboardExplanationDialogFragment();
                        keyboardDialog.show(getFragmentManager(), "keyboardExplanationDialog");
                    }
                    return false;
                });

                Preference keyboardSubPreference = findPreference(getString(R.string.magic_keyboard_preference_key));
                keyboardSubPreference.setOnPreferenceClickListener(preference -> {
                    Intent intentKeyboard = new Intent(getContext(), MagikIMESettings.class);
                    startActivity(intentKeyboard);
                    return false;
                });

                // Present in two places
                allowCopyPassword();

                break;

            case DATABASE:
                setPreferencesFromResource(R.xml.database_preferences, rootKey);

                if (database.getLoaded()) {

                    PreferenceCategory dbGeneralPrefCategory = (PreferenceCategory) findPreference(getString(R.string.database_general_key));

                    // Db name
                    Preference dbNamePref = findPreference(getString(R.string.database_name_key));
                    if ( database.containsName() ) {
                        dbNamePref.setSummary(database.getName());
                    } else {
                        dbGeneralPrefCategory.removePreference(dbNamePref);
                    }

                    // Db description
                    Preference dbDescriptionPref = findPreference(getString(R.string.database_description_key));
                    if ( database.containsDescription() ) {
                        dbDescriptionPref.setSummary(database.getDescription());
                    } else {
                        dbGeneralPrefCategory.removePreference(dbDescriptionPref);
                    }

                    // Recycle bin
                    SwitchPreference recycleBinPref = (SwitchPreference) findPreference(getString(R.string.recycle_bin_key));
                    // TODO Recycle
                    dbGeneralPrefCategory.removePreference(recycleBinPref); // To delete
                    if (database.isRecycleBinAvailable()) {

                        recycleBinPref.setChecked(database.isRecycleBinEnabled());
                        recycleBinPref.setEnabled(false);
                    } else {
                        dbGeneralPrefCategory.removePreference(recycleBinPref);
                    }

                    // Version
                    Preference dbVersionPref = findPreference(getString(R.string.database_version_key));
                    dbVersionPref.setSummary(database.getVersion());

                    // Encryption Algorithm
                    Preference algorithmPref = findPreference(getString(R.string.encryption_algorithm_key));
                    algorithmPref.setSummary(database.getEncryptionAlgorithmName(getResources()));

                    // Key derivation function
                    Preference kdfPref = findPreference(getString(R.string.key_derivation_function_key));
                    kdfPref.setSummary(database.getKeyDerivationName(getResources()));

                    // Round encryption
                    roundPref = findPreference(getString(R.string.transform_rounds_key));
                    roundPref.setSummary(database.getNumberKeyEncryptionRoundsAsString());

                    // Memory Usage
                    memoryPref = findPreference(getString(R.string.memory_usage_key));
                    memoryPref.setSummary(database.getMemoryUsageAsString());

                    // Parallelism
                    parallelismPref = findPreference(getString(R.string.parallelism_key));
                    parallelismPref.setSummary(database.getParallelismAsString());

                } else {
                    Log.e(getClass().getName(), "Database isn't ready");
                }

                break;

            case APPEARANCE:
                setPreferencesFromResource(R.xml.appearance_preferences, rootKey);

                Preference stylePreference = findPreference(getString(R.string.setting_style_key));
                stylePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String styleIdString = (String) newValue;
                    if (!(!BuildConfig.CLOSED_STORE && PreferencesUtil.isEducationScreenReclickedPerformed(getContext())))
                    for (String themeIdDisabled : BuildConfig.STYLES_DISABLED) {
                        if (themeIdDisabled.equals(styleIdString)) {
                            ProFeatureDialogFragment dialogFragment = new ProFeatureDialogFragment();
                            if (getFragmentManager() != null)
                                dialogFragment.show(getFragmentManager(), "pro_feature_dialog");
                            return false;
                        }
                    }

                    Stylish.assignStyle(styleIdString);
                    if (getActivity() != null)
                        getActivity().recreate();
                    return true;
                });

                Preference iconPackPreference = findPreference(getString(R.string.setting_icon_pack_choose_key));
                iconPackPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String iconPackId = (String) newValue;
                    if (!(!BuildConfig.CLOSED_STORE && PreferencesUtil.isEducationScreenReclickedPerformed(getContext())))
                    for (String iconPackIdDisabled : BuildConfig.ICON_PACKS_DISABLED) {
                        if (iconPackIdDisabled.equals(iconPackId)) {
                            ProFeatureDialogFragment dialogFragment = new ProFeatureDialogFragment();
                            if (getFragmentManager() != null)
                                dialogFragment.show(getFragmentManager(), "pro_feature_dialog");
                            return false;
                        }
                    }

                    IconPackChooser.setSelectedIconPack(iconPackId);
                    return true;
                });

                Preference resetEducationScreens = findPreference(getString(R.string.reset_education_screens_key));
                resetEducationScreens.setOnPreferenceClickListener(preference -> {
                    // To allow only one toast
                    if (count == 0) {
                        SharedPreferences sharedPreferences = PreferencesUtil.getEducationSharedPreferences(getContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        for (int resourceId : PreferencesUtil.educationResourceKeys) {
                            editor.putBoolean(getString(resourceId), false);
                        }
                        editor.apply();
                        Toast.makeText(getContext(), R.string.reset_education_screens_text, Toast.LENGTH_SHORT).show();
                    }
                    count++;
                    return false;
                });

                break;

            default:
                break;
        }
    }

    private void allowCopyPassword() {
        SwitchPreference copyPasswordPreference = (SwitchPreference) findPreference(getString(R.string.allow_copy_password_key));
        copyPasswordPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue && getContext() != null) {
                String message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning);
                AlertDialog warningDialog = new AlertDialog.Builder(getContext())
                        .setMessage(message).create();
                warningDialog.setButton(AlertDialog.BUTTON1, getText(android.R.string.ok),
                        (dialog, which) -> dialog.dismiss());
                warningDialog.setButton(AlertDialog.BUTTON2, getText(android.R.string.cancel),
                        (dialog, which) -> {
                            copyPasswordPreference.setChecked(false);
                            dialog.dismiss();
                        });
                warningDialog.show();
            }
            return true;
        });
    }

    private void preferenceInDevelopment(Preference preferenceInDev) {
        preferenceInDev.setOnPreferenceClickListener(preference -> {
            FragmentManager fragmentManager = getFragmentManager();
            assert fragmentManager != null;
            try { // don't check if we can
                ((SwitchPreference) preference).setChecked(false);
            } catch (Exception ignored) {}
            new UnderDevelopmentFeatureDialogFragment().show(getFragmentManager(), "underDevFeatureDialog");
            return false;
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if(count==10) {
            if (getActivity()!=null)
                PreferencesUtil.getEducationSharedPreferences(getActivity()).edit()
                    .putBoolean(getString(R.string.education_screen_reclicked_key), true).apply();
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        assert getFragmentManager() != null;

        boolean otherDialogFragment = false;

        DialogFragment dialogFragment = null;
        if (preference.getKey().equals(getString(R.string.database_name_key))) {
            dialogFragment = DatabaseNamePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference.getKey().equals(getString(R.string.database_description_key))) {
            dialogFragment = DatabaseDescriptionPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference.getKey().equals(getString(R.string.encryption_algorithm_key))) {
            dialogFragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference.getKey().equals(getString(R.string.key_derivation_function_key))) {
            DatabaseKeyDerivationPreferenceDialogFragmentCompat keyDerivationDialogFragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            // Add other prefs to manage
            if (roundPref != null)
                keyDerivationDialogFragment.setRoundPreference(roundPref);
            if (memoryPref != null)
                keyDerivationDialogFragment.setMemoryPreference(memoryPref);
            if (parallelismPref != null)
                keyDerivationDialogFragment.setParallelismPreference(parallelismPref);
            dialogFragment = keyDerivationDialogFragment;
        } else if (preference.getKey().equals(getString(R.string.transform_rounds_key))) {
            dialogFragment = RoundsPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference.getKey().equals(getString(R.string.memory_usage_key))) {
            dialogFragment = MemoryUsagePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference.getKey().equals(getString(R.string.parallelism_key))) {
            dialogFragment = ParallelismPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else {
            otherDialogFragment = true;
        }

        if (dialogFragment != null && !databaseReadOnly) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        }

        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    public static String retrieveTitle(Resources resources, Screen key) {
        switch (key) {
            case APPLICATION:
                return resources.getString(R.string.menu_app_settings);
            case FORM_FILLING:
                return resources.getString(R.string.menu_form_filling_settings);
            case DATABASE:
                return resources.getString(R.string.menu_db_settings);
            case APPEARANCE:
                return resources.getString(R.string.appearance);
            default:
                return resources.getString(R.string.settings);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        ReadOnlyHelper.onSaveInstanceState(outState, databaseReadOnly);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO encapsulate

        return false;
    }
}
