/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
import androidx.fragment.app.FragmentActivity
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
import com.kunzisoft.keepass.notifications.AdvancedUnlockNotificationService
import com.kunzisoft.keepass.settings.preference.IconPackListPreference
import com.kunzisoft.keepass.utils.UriUtil


class NestedAppSettingsFragment : NestedSettingsFragment() {

    private var deleteKeysAlertDialog: AlertDialog? = null

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
            findPreference<Preference>(getString(R.string.remember_database_locations_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAll()
                }
                true
            }

            findPreference<Preference>(getString(R.string.remember_keyfile_locations_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAllKeyFiles()
                }
                true
            }
        }
    }

    private fun onCreateFormFillingPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_form_filling, rootKey)

        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autoFillEnablePreference: SwitchPreference? = findPreference(getString(R.string.settings_autofill_enable_key))
                val autofillManager = activity.getSystemService(AutofillManager::class.java)
                if (autofillManager != null && autofillManager.hasEnabledAutofillServices())
                    autoFillEnablePreference?.isChecked = autofillManager.hasEnabledAutofillServices()
                autoFillEnablePreference?.onPreferenceClickListener = object : Preference.OnPreferenceClickListener {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        if ((preference as SwitchPreference).isChecked) {
                            try {
                                enableService()
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
                            Log.d(javaClass.name, "Autofill service already disabled.")
                        }
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Throws(ActivityNotFoundException::class)
                    private fun enableService() {
                        if (autofillManager != null && !autofillManager.hasEnabledAutofillServices()) {
                            val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                            intent.data = Uri.parse("package:com.kunzisoft.keepass.autofill.KeeAutofillService")
                            Log.d(javaClass.name, "Autofill enable service: intent=$intent")
                            startActivityForResult(intent, REQUEST_CODE_AUTOFILL)
                        } else {
                            Log.d(javaClass.name, "Autofill service already enabled.")
                        }
                    }
                }
            } else {
                findPreference<Preference>(getString(R.string.autofill_key))?.isVisible = false
            }
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(requireContext(), R.string.magic_keyboard_explanation_url)
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_key))?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_preference_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, MagikeyboardSettingsActivity::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.autofill_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(requireContext(), R.string.autofill_explanation_url)
            false
        }

        findPreference<Preference>(getString(R.string.settings_autofill_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, AutofillSettingsActivity::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.clipboard_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(requireContext(), R.string.clipboard_explanation_url)
            false
        }

        val copyPasswordPreference: SwitchPreference? = findPreference(getString(R.string.allow_copy_password_key))
        copyPasswordPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && context != null) {
                val message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning)
                AlertDialog
                        .Builder(requireContext())
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
            val deviceCredentialUnlockEnablePreference: SwitchPreference? = findPreference(getString(R.string.device_credential_unlock_enable_key))
            val autoOpenPromptPreference: SwitchPreference? = findPreference(getString(R.string.biometric_auto_open_prompt_key))
            val tempAdvancedUnlockPreference: SwitchPreference? = findPreference(getString(R.string.temp_advanced_unlock_enable_key))

            val biometricUnlockSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BiometricUnlockDatabaseHelper.biometricUnlockSupported(activity)
            } else false
            biometricUnlockEnablePreference?.apply {
                // False if under Marshmallow
                if (!biometricUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as SwitchPreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                                .show(parentFragmentManager, "unavailableFeatureDialog")
                        false
                    }
                } else {
                    setOnPreferenceClickListener {
                        val biometricChecked = biometricUnlockEnablePreference.isChecked
                        val deviceCredentialChecked = deviceCredentialUnlockEnablePreference?.isChecked ?: false
                        if (!biometricChecked) {
                            biometricUnlockEnablePreference.isChecked = true
                            deleteKeysMessage(activity) {
                                biometricUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = deviceCredentialChecked
                                tempAdvancedUnlockPreference?.isEnabled = deviceCredentialChecked
                            }
                        } else {
                            if (deviceCredentialChecked) {
                                biometricUnlockEnablePreference.isChecked = false
                                deleteKeysMessage(activity) {
                                    biometricUnlockEnablePreference.isChecked = true
                                    deviceCredentialUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                autoOpenPromptPreference?.isEnabled = true
                                tempAdvancedUnlockPreference?.isEnabled = true
                            }
                        }
                        true
                    }
                }
            }

            val deviceCredentialUnlockSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricUnlockDatabaseHelper.deviceCredentialUnlockSupported(activity)
            } else false
            deviceCredentialUnlockEnablePreference?.apply {
                if (!deviceCredentialUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as SwitchPreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.R)
                                .show(parentFragmentManager, "unavailableFeatureDialog")
                        false
                    }
                } else {
                    setOnPreferenceClickListener {
                        val deviceCredentialChecked = deviceCredentialUnlockEnablePreference.isChecked
                        val biometricChecked = biometricUnlockEnablePreference?.isChecked ?: false
                        if (!deviceCredentialChecked) {
                            deviceCredentialUnlockEnablePreference.isChecked = true
                            deleteKeysMessage(activity) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = biometricChecked
                                tempAdvancedUnlockPreference?.isEnabled = biometricChecked
                            }
                        } else {
                            if (biometricChecked) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                deleteKeysMessage(activity) {
                                    deviceCredentialUnlockEnablePreference.isChecked = true
                                    biometricUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                autoOpenPromptPreference?.isEnabled = true
                                tempAdvancedUnlockPreference?.isEnabled = true
                            }
                        }
                        true
                    }
                }
            }

            autoOpenPromptPreference?.isEnabled = biometricUnlockEnablePreference?.isChecked == true
                        || deviceCredentialUnlockEnablePreference?.isChecked == true
            tempAdvancedUnlockPreference?.isEnabled = biometricUnlockEnablePreference?.isChecked == true
                    || deviceCredentialUnlockEnablePreference?.isChecked == true

            tempAdvancedUnlockPreference?.setOnPreferenceClickListener {
                tempAdvancedUnlockPreference.isChecked = !tempAdvancedUnlockPreference.isChecked
                deleteKeysMessage(activity) {
                    tempAdvancedUnlockPreference.isChecked = !tempAdvancedUnlockPreference.isChecked
                }
                true
            }

            val deleteKeysFingerprints: Preference? = findPreference(getString(R.string.biometric_delete_all_key_key))
            if (biometricUnlockSupported || deviceCredentialUnlockSupported) {
                deleteKeysFingerprints?.setOnPreferenceClickListener {
                    deleteKeysMessage(activity)
                    false
                }
            } else {
                deleteKeysFingerprints?.isEnabled = false
            }
        }

        findPreference<Preference>(getString(R.string.advanced_unlock_explanation_key))?.setOnPreferenceClickListener {
            UriUtil.gotoUrl(requireContext(), R.string.advanced_unlock_explanation_url)
            false
        }
    }

    private fun deleteKeysMessage(activity: FragmentActivity, validate: (()->Unit)? = null) {
        deleteKeysAlertDialog = AlertDialog.Builder(activity)
                .setMessage(resources.getString(R.string.advanced_unlock_delete_all_key_warning))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(resources.getString(android.R.string.ok)
                ) { _, _ ->
                    validate?.invoke()
                    deleteKeysAlertDialog?.setOnDismissListener(null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        BiometricUnlockDatabaseHelper.deleteEntryKeyInKeystoreForBiometric(
                                activity,
                                object : BiometricUnlockDatabaseHelper.BiometricUnlockErrorCallback {
                                    fun showException(e: Exception) {
                                        Toast.makeText(context,
                                                getString(R.string.advanced_unlock_scanning_error, e.localizedMessage),
                                                Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onInvalidKeyException(e: Exception) {
                                        showException(e)
                                    }

                                    override fun onBiometricException(e: Exception) {
                                        showException(e)
                                    }
                                })
                    }
                    AdvancedUnlockNotificationService.stopService(activity.applicationContext)
                    CipherDatabaseAction.getInstance(activity.applicationContext).deleteAll()
                }
                .setNegativeButton(resources.getString(android.R.string.cancel)
                ) { _, _ ->}
                .create()
        deleteKeysAlertDialog?.show()
    }

    private fun onCreateAppearancePreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        // To change list items appearance
        PreferencesUtil.APPEARANCE_CHANGED = true

        activity?.let { activity ->
            findPreference<ListPreference>(getString(R.string.setting_style_key))?.setOnPreferenceChangeListener { _, newValue ->
                var styleEnabled = true
                val styleIdString = newValue as String
                if (BuildConfig.CLOSED_STORE || !Education.isEducationScreenReclickedPerformed(activity))
                    for (themeIdDisabled in BuildConfig.STYLES_DISABLED) {
                        if (themeIdDisabled == styleIdString) {
                            styleEnabled = false
                            ProFeatureDialogFragment().show(parentFragmentManager, "pro_feature_dialog")
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
                if (BuildConfig.CLOSED_STORE || !Education.isEducationScreenReclickedPerformed(activity))
                    for (iconPackIdDisabled in BuildConfig.ICON_PACKS_DISABLED) {
                        if (iconPackIdDisabled == iconPackId) {
                            iconPackEnabled = false
                            ProFeatureDialogFragment().show(parentFragmentManager, "pro_feature_dialog")
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
                    val sharedPreferences = Education.getEducationSharedPreferences(activity)
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
                findPreference<SwitchPreference?>(getString(R.string.settings_autofill_enable_key))?.let { autoFillEnablePreference ->
                    val autofillManager = activity.getSystemService(AutofillManager::class.java)
                    autoFillEnablePreference.isChecked = autofillManager != null
                            && autofillManager.hasEnabledAutofillServices()
                }
            }
        }
    }

    override fun onPause() {
        deleteKeysAlertDialog?.dismiss()
        super.onPause()
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