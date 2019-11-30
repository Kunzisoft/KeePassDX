package com.kunzisoft.keepass.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.ProFeatureDialogFragment
import com.kunzisoft.keepass.activities.dialogs.UnavailableFeatureDialogFragment
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.biometric.BiometricUnlockDatabaseHelper
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.settings.preference.IconPackListPreference
import com.kunzisoft.keepass.utils.UriUtil

class NestedAppSettingsFragment : NestedSettingsFragment() {

    override fun onCreateScreenPreference(screen: Screen, savedInstanceState: Bundle?, rootKey: String?) {

        // Load the preferences from an XML resource
        when (screen) {
            Screen.APPLICATION -> {
                onCreateApplicationPreferences(rootKey)
            }
            Screen.FORM_FILLING -> {
                onCreateFormFillingPreference(rootKey)
            }
            Screen.ADVANCED_UNLOCK -> {
                onCreateAdvancedUnlockPreferences(rootKey)
            }
            Screen.APPEARANCE -> {
                onCreateAppearancePreferences(rootKey)
            }
            else -> {}
        }
    }

    private fun onCreateApplicationPreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_application, rootKey)

        activity?.let { activity ->
            allowCopyPassword()

            findPreference<Preference>(getString(R.string.keyfile_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAllKeyFiles()
                }
                true
            }

            findPreference<Preference>(getString(R.string.recentfile_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAll()
                }
                true
            }
        }
    }

    private fun onCreateFormFillingPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_form_filling, rootKey)

        activity?.let { activity ->
            val autoFillEnablePreference: SwitchPreference? = findPreference(getString(R.string.settings_autofill_enable_key))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autofillManager = activity.getSystemService(AutofillManager::class.java)
                if (autofillManager != null && autofillManager.hasEnabledAutofillServices())
                    autoFillEnablePreference?.isChecked = autofillManager.hasEnabledAutofillServices()
                autoFillEnablePreference?.onPreferenceClickListener = object : Preference.OnPreferenceClickListener {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        if ((preference as SwitchPreference).isChecked) {
                            try {
                                startEnableService()
                            } catch (e: ActivityNotFoundException) {
                                val error = getString(R.string.error_autofill_enable_service)
                                preference.isChecked = false
                                Log.d(javaClass.name, error, e)
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            disableService()
                        }
                        return false
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    private fun disableService() {
                        if (autofillManager != null && autofillManager.hasEnabledAutofillServices()) {
                            autofillManager.disableAutofillServices()
                        } else {
                            Log.d(javaClass.name, "Sample service already disabled.")
                        }
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Throws(ActivityNotFoundException::class)
                    private fun startEnableService() {
                        if (autofillManager != null && !autofillManager.hasEnabledAutofillServices()) {
                            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                            // TODO Autofill
                            intent.data = Uri.parse("package:com.example.android.autofill.service")
                            Log.d(javaClass.name, "enableService(): intent=$intent")
                            startActivityForResult(intent, REQUEST_CODE_AUTOFILL)
                        } else {
                            Log.d(javaClass.name, "Sample service already enabled.")
                        }
                    }
                }
            } else {
                autoFillEnablePreference?.setOnPreferenceClickListener { preference ->
                    (preference as SwitchPreference).isChecked = false
                    val fragmentManager = fragmentManager!!
                    UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.O)
                            .show(fragmentManager, "unavailableFeatureDialog")
                    false
                }
            }
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(context!!, R.string.magic_keyboard_explanation_url)
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_key))?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_preference_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, MagikIMESettings::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.clipboard_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(context!!, R.string.clipboard_explanation_url)
            false
        }

        findPreference<Preference>(getString(R.string.autofill_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(context!!, R.string.autofill_explanation_url)
            false
        }

        // Present in two places
        allowCopyPassword()
    }

    private fun allowCopyPassword() {
        val copyPasswordPreference: SwitchPreference? = findPreference(getString(R.string.allow_copy_password_key))
        copyPasswordPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && context != null) {
                val message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning)
                AlertDialog
                        .Builder(context!!)
                        .setMessage(message)
                        .create()
                        .apply {
                            setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.enable))
                            { dialog, _ ->
                                dialog.dismiss()
                            }
                            setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable))
                            { dialog, _ ->
                                copyPasswordPreference.isChecked = false
                                dialog.dismiss()
                            }
                            show()
                        }
            }
            true
        }
    }

    private fun onCreateAdvancedUnlockPreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced_unlock, rootKey)

        activity?.let { activity ->
            val biometricUnlockEnablePreference: SwitchPreference? = findPreference(getString(R.string.biometric_unlock_enable_key))
            // < M solve verifyError exception
            var biometricUnlockSupported = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val biometricCanAuthenticate = BiometricManager.from(activity).canAuthenticate()
                biometricUnlockSupported = biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                        || biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
            }
            if (!biometricUnlockSupported) {
                // False if under Marshmallow
                biometricUnlockEnablePreference?.apply {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        fragmentManager?.let { fragmentManager ->
                            (preference as SwitchPreference).isChecked = false
                            UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                                    .show(fragmentManager, "unavailableFeatureDialog")
                        }
                        false
                    }
                }
            }

            val deleteKeysFingerprints: Preference? = findPreference(getString(R.string.biometric_delete_all_key_key))
            if (!biometricUnlockSupported) {
                deleteKeysFingerprints?.isEnabled = false
            } else {
                deleteKeysFingerprints?.setOnPreferenceClickListener {
                    context?.let { context ->
                        AlertDialog.Builder(context)
                                .setMessage(resources.getString(R.string.biometric_delete_all_key_warning))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(resources.getString(android.R.string.yes)
                                ) { _, _ ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        BiometricUnlockDatabaseHelper.deleteEntryKeyInKeystoreForBiometric(
                                                activity,
                                                object : BiometricUnlockDatabaseHelper.BiometricUnlockErrorCallback {
                                                    override fun onInvalidKeyException(e: Exception) {}

                                                    override fun onBiometricException(e: Exception) {
                                                        Toast.makeText(context,
                                                                getString(R.string.biometric_scanning_error, e.localizedMessage),
                                                                Toast.LENGTH_SHORT).show()
                                                    }
                                                })
                                    }
                                    CipherDatabaseAction.getInstance(context.applicationContext).deleteAll()
                                }
                                .setNegativeButton(resources.getString(android.R.string.no))
                                { _, _ -> }.show()
                    }
                    false
                }
            }
        }

        findPreference<Preference>(getString(R.string.advanced_unlock_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(context!!, R.string.advanced_unlock_explanation_url)
            false
        }
    }

    private fun onCreateAppearancePreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        activity?.let { activity ->
            findPreference<ListPreference>(getString(R.string.setting_style_key))?.setOnPreferenceChangeListener { _, newValue ->
                var styleEnabled = true
                val styleIdString = newValue as String
                if (BuildConfig.CLOSED_STORE || !Education.isEducationScreenReclickedPerformed(context!!))
                    for (themeIdDisabled in BuildConfig.STYLES_DISABLED) {
                        if (themeIdDisabled == styleIdString) {
                            styleEnabled = false
                            fragmentManager?.let { fragmentManager ->
                                ProFeatureDialogFragment().show(fragmentManager, "pro_feature_dialog")
                            }
                        }
                    }
                if (styleEnabled) {
                    Stylish.assignStyle(styleIdString)
                    activity.recreate()
                }
                styleEnabled
            }

            findPreference<IconPackListPreference>(getString(R.string.setting_icon_pack_choose_key))?.setOnPreferenceChangeListener { _, newValue ->
                var iconPackEnabled = true
                val iconPackId = newValue as String
                if (BuildConfig.CLOSED_STORE || !Education.isEducationScreenReclickedPerformed(context!!))
                    for (iconPackIdDisabled in BuildConfig.ICON_PACKS_DISABLED) {
                        if (iconPackIdDisabled == iconPackId) {
                            iconPackEnabled = false
                            fragmentManager?.let { fragmentManager ->
                                ProFeatureDialogFragment().show(fragmentManager, "pro_feature_dialog")
                            }
                        }
                    }
                if (iconPackEnabled) {
                    IconPackChooser.setSelectedIconPack(iconPackId)
                }
                iconPackEnabled
            }

            findPreference<Preference>(getString(R.string.reset_education_screens_key))?.setOnPreferenceClickListener {
                // To allow only one toast
                if (mCount == 0) {
                    val sharedPreferences = Education.getEducationSharedPreferences(context!!)
                    val editor = sharedPreferences.edit()
                    for (resourceId in Education.educationResourcesKeys) {
                        editor.putBoolean(getString(resourceId), false)
                    }
                    editor.apply()
                    Toast.makeText(context, R.string.reset_education_screens_text, Toast.LENGTH_SHORT).show()
                }
                mCount++
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autoFillEnablePreference: SwitchPreference? = findPreference(getString(R.string.settings_autofill_enable_key))
                if (autoFillEnablePreference != null) {
                    val autofillManager = activity.getSystemService(AutofillManager::class.java)
                    autoFillEnablePreference.isChecked = autofillManager != null
                            && autofillManager.hasEnabledAutofillServices()
                }
            }
        }
    }

    private var mCount = 0
    override fun onStop() {
        super.onStop()
        activity?.let { activity ->
            if (mCount == 10) {
                Education.getEducationSharedPreferences(activity).edit()
                        .putBoolean(getString(R.string.education_screen_reclicked_key), true).apply()
            }
        }
    }

    companion object {

        private const val REQUEST_CODE_AUTOFILL = 5201
    }
}