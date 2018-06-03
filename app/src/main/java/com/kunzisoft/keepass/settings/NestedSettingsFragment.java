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
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.database.Database;
import com.kunzisoft.keepass.dialogs.ProFeatureDialogFragment;
import com.kunzisoft.keepass.dialogs.StorageAccessFrameworkDialog;
import com.kunzisoft.keepass.dialogs.UnavailableFeatureDialogFragment;
import com.kunzisoft.keepass.dialogs.UnderDevelopmentFeatureDialogFragment;
import com.kunzisoft.keepass.fingerprint.FingerPrintHelper;
import com.kunzisoft.keepass.icons.IconPackChooser;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseDescriptionPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseKeyDerivationPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.DatabaseNamePreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.MemoryUsagePreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.ParallelismPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.settings.preferenceDialogFragment.RoundsPreferenceDialogFragmentCompat;
import com.kunzisoft.keepass.stylish.Stylish;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class NestedSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    public enum Screen {
        APPLICATION, FORM_FILLING, DATABASE, APPEARANCE
    }

    private static final String TAG_KEY = "NESTED_KEY";

    private static final int REQUEST_CODE_AUTOFILL = 5201;

    private int count = 0;

    private Preference roundPref;
    private Preference memoryPref;
    private Preference parallelismPref;

    public static NestedSettingsFragment newInstance(Screen key) {
        NestedSettingsFragment fragment = new NestedSettingsFragment();
        // supply arguments to bundle.
        Bundle args = new Bundle();
        args.putInt(TAG_KEY, key.ordinal());
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
        int key = 0;
        if (getArguments() != null)
            key = getArguments().getInt(TAG_KEY);

        // Load the preferences from an XML resource
        switch (Screen.values()[key]) {
            case APPLICATION:
                setPreferencesFromResource(R.xml.application_preferences, rootKey);

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

                // TODO define the checkbox by verifying
                SwitchPreference keyboardPreference = (SwitchPreference) findPreference(getString(R.string.magic_keyboard_key));
                keyboardPreference.setOnPreferenceClickListener(preference -> {
                    if (getContext() != null) {
                        InputMethodManager imeManager = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                        if (imeManager != null)
                            imeManager.showInputMethodPicker();
                    }
                    return false;
                });

                break;

            case DATABASE:
                setPreferencesFromResource(R.xml.database_preferences, rootKey);

                Database db = App.getDB();
                if (db.getLoaded()) {

                    PreferenceCategory dbGeneralPrefCategory = (PreferenceCategory) findPreference(getString(R.string.database_general_key));

                    // Db name
                    Preference dbNamePref = findPreference(getString(R.string.database_name_key));
                    if ( db.containsName() ) {
                        dbNamePref.setSummary(db.getName());
                    } else {
                        dbGeneralPrefCategory.removePreference(dbNamePref);
                    }

                    // Db description
                    Preference dbDescriptionPref = findPreference(getString(R.string.database_description_key));
                    if ( db.containsDescription() ) {
                        dbDescriptionPref.setSummary(db.getDescription());
                    } else {
                        dbGeneralPrefCategory.removePreference(dbDescriptionPref);
                    }

                    // Recycle bin
                    SwitchPreference recycleBinPref = (SwitchPreference) findPreference(getString(R.string.recycle_bin_key));
                    // TODO Recycle
                    dbGeneralPrefCategory.removePreference(recycleBinPref); // To delete
                    if (db.isRecycleBinAvailable()) {

                        recycleBinPref.setChecked(db.isRecycleBinEnabled());
                        recycleBinPref.setEnabled(false);
                    } else {
                        dbGeneralPrefCategory.removePreference(recycleBinPref);
                    }

                    // Version
                    Preference dbVersionPref = findPreference(getString(R.string.database_version_key));
                    dbVersionPref.setSummary(db.getVersion());

                    // Encryption Algorithm
                    Preference algorithmPref = findPreference(getString(R.string.encryption_algorithm_key));
                    algorithmPref.setSummary(db.getEncryptionAlgorithmName(getResources()));

                    // Key derivation function
                    Preference kdfPref = findPreference(getString(R.string.key_derivation_function_key));
                    kdfPref.setSummary(db.getKeyDerivationName(getResources()));

                    // Round encryption
                    roundPref = findPreference(getString(R.string.transform_rounds_key));
                    roundPref.setSummary(db.getNumberKeyEncryptionRoundsAsString());

                    // Memory Usage
                    memoryPref = findPreference(getString(R.string.memory_usage_key));
                    memoryPref.setSummary(db.getMemoryUsageAsString());

                    // Parallelism
                    parallelismPref = findPreference(getString(R.string.parallelism_key));
                    parallelismPref.setSummary(db.getParallelismAsString());

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

        DialogFragment dialogFragment = null;

        if (preference.getKey().equals(getString(R.string.database_name_key))) {
            dialogFragment = DatabaseNamePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        else if (preference.getKey().equals(getString(R.string.database_description_key))) {
            dialogFragment = DatabaseDescriptionPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        else if (preference.getKey().equals(getString(R.string.encryption_algorithm_key))) {
            dialogFragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        else if (preference.getKey().equals(getString(R.string.key_derivation_function_key))) {
            DatabaseKeyDerivationPreferenceDialogFragmentCompat keyDerivationDialogFragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            // Add other prefs to manage
            if (roundPref != null)
                keyDerivationDialogFragment.setRoundPreference(roundPref);
            if (memoryPref != null)
                keyDerivationDialogFragment.setMemoryPreference(memoryPref);
            if (parallelismPref != null)
                keyDerivationDialogFragment.setParallelismPreference(parallelismPref);
            dialogFragment = keyDerivationDialogFragment;
        }
        else if (preference.getKey().equals(getString(R.string.transform_rounds_key))) {
            dialogFragment = RoundsPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        else if (preference.getKey().equals(getString(R.string.memory_usage_key))) {
            dialogFragment = MemoryUsagePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }
        else if (preference.getKey().equals(getString(R.string.parallelism_key))) {
            dialogFragment = ParallelismPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), null);
        }

        // Could not be handled here. Try with the super method.
        else {
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
    public boolean onPreferenceClick(Preference preference) {
        // TODO encapsulate

        return false;
    }
}
