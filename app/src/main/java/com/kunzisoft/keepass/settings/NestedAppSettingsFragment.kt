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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.ProFeatureDialogFragment
import com.kunzisoft.keepass.activities.dialogs.UnavailableFeatureDialogFragment
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.biometric.DeviceUnlockManager
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.settings.preference.IconPackListPreference
import com.kunzisoft.keepass.settings.preferencedialogfragment.DurationDialogFragmentCompat
import com.kunzisoft.keepass.utils.UriUtil.isContributingUser
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.utils.UriUtil.releaseAllUnnecessaryPermissionUris


class NestedAppSettingsFragment : NestedSettingsFragment() {

    private var warningAlertDialog: AlertDialog? = null

    override fun onCreateScreenPreference(screen: Screen, savedInstanceState: Bundle?, rootKey: String?) {

        // Load the preferences from an XML resource
        when (screen) {
            Screen.APPLICATION -> {
                onCreateApplicationPreferences(rootKey)
            }
            Screen.FORM_FILLING -> {
                onCreateFormFillingPreference(rootKey)
            }
            Screen.DEVICE_UNLOCK -> {
                onCreateDeviceUnlockPreferences(rootKey)
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
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAll {
                        activity.releaseAllUnnecessaryPermissionUris()
                    }
                }
                true
            }

            findPreference<Preference>(getString(R.string.remember_keyfile_locations_key))?.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistoryAction.getInstance(activity.applicationContext).deleteAllKeyFiles {
                        activity.releaseAllUnnecessaryPermissionUris()
                    }
                }
                true
            }

            findPreference<Preference>(getString(R.string.import_app_properties_key))?.setOnPreferenceClickListener { _ ->
                (activity as? SettingsActivity?)?.apply {
                    importAppProperties()
                }
                true
            }

            findPreference<Preference>(getString(R.string.export_app_properties_key))?.setOnPreferenceClickListener { _ ->
                (activity as? SettingsActivity?)?.apply {
                    exportAppProperties()
                }
                true
            }
        }
    }

    private fun onCreateFormFillingPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_form_filling, rootKey)

        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autoFillEnablePreference: TwoStatePreference? = findPreference(getString(R.string.settings_autofill_enable_key))
                activity.getSystemService(AutofillManager::class.java)?.let { autofillManager ->
                    if (autofillManager.hasEnabledAutofillServices())
                        autoFillEnablePreference?.isChecked = autofillManager.hasEnabledAutofillServices()

                    autoFillEnablePreference?.onPreferenceClickListener =
                        object : Preference.OnPreferenceClickListener {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            override fun onPreferenceClick(preference: Preference): Boolean {
                                if ((preference as TwoStatePreference).isChecked) {
                                    try {
                                        enableService()
                                    } catch (e: ActivityNotFoundException) {
                                        val error =
                                            getString(R.string.error_autofill_enable_service)
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
                                if (autofillManager.hasEnabledAutofillServices()) {
                                    autofillManager.disableAutofillServices()
                                } else {
                                    Log.d(javaClass.name, "Autofill service already disabled.")
                                }
                            }

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Throws(ActivityNotFoundException::class)
                            private fun enableService() {
                                if (!autofillManager.hasEnabledAutofillServices()) {
                                    val intent =
                                        Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                                    intent.data =
                                        Uri.parse("package:com.kunzisoft.keepass.autofill.KeeAutofillService")
                                    Log.d(javaClass.name, "Autofill enable service: intent=$intent")
                                    startActivity(intent)
                                } else {
                                    Log.d(javaClass.name, "Autofill service already enabled.")
                                }
                            }
                        }
                }
            } else {
                findPreference<Preference>(getString(R.string.autofill_key))?.isVisible = false
            }
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_explanation_key))?.setOnPreferenceClickListener {
            context?.openUrl(R.string.magic_keyboard_explanation_url)
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
            context?.openUrl(R.string.autofill_explanation_url)
            false
        }

        findPreference<Preference>(getString(R.string.settings_autofill_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, AutofillSettingsActivity::class.java))
            false
        }

        findPreference<Preference>(getString(R.string.clipboard_notifications_key))?.setOnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                ClipboardEntryNotificationService.removeNotification(context)
            }
            true
        }

        findPreference<Preference>(getString(R.string.clipboard_explanation_key))?.setOnPreferenceClickListener {
            context?.openUrl(R.string.clipboard_explanation_url)
            false
        }

        val copyPasswordPreference: TwoStatePreference? = findPreference(getString(R.string.allow_copy_password_key))
        copyPasswordPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && context != null) {
                val message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning)
                AlertDialog.Builder(requireContext())
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

    private fun onCreateDeviceUnlockPreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_device_unlock, rootKey)

        activity?.let { activity ->

            val biometricUnlockEnablePreference: TwoStatePreference? = findPreference(getString(R.string.biometric_unlock_enable_key))
            val deviceCredentialUnlockEnablePreference: TwoStatePreference? = findPreference(getString(R.string.device_credential_unlock_enable_key))
            val autoOpenPromptPreference: TwoStatePreference? = findPreference(getString(R.string.biometric_auto_open_prompt_key))
            val tempDeviceUnlockPreference: TwoStatePreference? = findPreference(getString(R.string.temp_device_unlock_enable_key))

            val biometricUnlockSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                DeviceUnlockManager.biometricUnlockSupported(activity)
            } else false
            biometricUnlockEnablePreference?.apply {
                // False if under Marshmallow
                if (!biometricUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as TwoStatePreference).isChecked = false
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
                            warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                                biometricUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = deviceCredentialChecked
                                tempDeviceUnlockPreference?.isEnabled = deviceCredentialChecked
                            }
                        } else {
                            if (deviceCredentialChecked) {
                                biometricUnlockEnablePreference.isChecked = false
                                warningMessage(activity, keystoreWarning = true, deleteKeys = true) {
                                    biometricUnlockEnablePreference.isChecked = true
                                    deviceCredentialUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                biometricUnlockEnablePreference.isChecked = false
                                warningMessage(activity, keystoreWarning = true, deleteKeys = false) {
                                    biometricUnlockEnablePreference.isChecked = true
                                    autoOpenPromptPreference?.isEnabled = true
                                    tempDeviceUnlockPreference?.isEnabled = true
                                }
                            }
                        }
                        true
                    }
                }
            }

            val deviceCredentialUnlockSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                DeviceUnlockManager.deviceCredentialUnlockSupported(activity)
            } else false
            deviceCredentialUnlockEnablePreference?.apply {
                // Biometric unlock already checked
                if (biometricUnlockEnablePreference?.isChecked == true)
                    isChecked = false
                if (!deviceCredentialUnlockSupported) {
                    isChecked = false
                    setOnPreferenceClickListener { preference ->
                        (preference as TwoStatePreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                                .show(parentFragmentManager, "unavailableFeatureDialog")
                        false
                    }
                } else {
                    setOnPreferenceClickListener {
                        val deviceCredentialChecked = deviceCredentialUnlockEnablePreference.isChecked
                        val biometricChecked = biometricUnlockEnablePreference?.isChecked ?: false
                        if (!deviceCredentialChecked) {
                            deviceCredentialUnlockEnablePreference.isChecked = true
                            warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                autoOpenPromptPreference?.isEnabled = biometricChecked
                                tempDeviceUnlockPreference?.isEnabled = biometricChecked
                            }
                        } else {
                            if (biometricChecked) {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                warningMessage(activity, keystoreWarning = true, deleteKeys = true) {
                                    deviceCredentialUnlockEnablePreference.isChecked = true
                                    biometricUnlockEnablePreference?.isChecked = false
                                }
                            } else {
                                deviceCredentialUnlockEnablePreference.isChecked = false
                                warningMessage(activity, keystoreWarning = true, deleteKeys = false) {
                                    deviceCredentialUnlockEnablePreference.isChecked = true
                                    autoOpenPromptPreference?.isEnabled = true
                                    tempDeviceUnlockPreference?.isEnabled = true
                                }
                            }
                        }
                        true
                    }
                }
            }

            autoOpenPromptPreference?.isEnabled = biometricUnlockEnablePreference?.isChecked == true
                        || deviceCredentialUnlockEnablePreference?.isChecked == true
            tempDeviceUnlockPreference?.isEnabled = biometricUnlockEnablePreference?.isChecked == true
                    || deviceCredentialUnlockEnablePreference?.isChecked == true

            tempDeviceUnlockPreference?.setOnPreferenceClickListener {
                tempDeviceUnlockPreference.isChecked = !tempDeviceUnlockPreference.isChecked
                warningMessage(activity, keystoreWarning = false, deleteKeys = true) {
                    tempDeviceUnlockPreference.isChecked = !tempDeviceUnlockPreference.isChecked
                }
                true
            }

            val deleteKeysFingerprints: Preference? = findPreference(getString(R.string.biometric_delete_all_key_key))
            if (biometricUnlockSupported || deviceCredentialUnlockSupported) {
                deleteKeysFingerprints?.setOnPreferenceClickListener {
                    warningMessage(activity, keystoreWarning = false, deleteKeys = true)
                    false
                }
            } else {
                deleteKeysFingerprints?.isEnabled = false
            }
        }

        findPreference<Preference>(getString(R.string.device_unlock_explanation_key))?.setOnPreferenceClickListener {
            context?.openUrl(R.string.device_unlock_explanation_url)
            false
        }
    }

    private fun warningMessage(activity: FragmentActivity,
                               keystoreWarning: Boolean,
                               deleteKeys: Boolean,
                               validate: (()->Unit)? = null) {
        var message = ""
        if (keystoreWarning) {
            message += resources.getString(R.string.device_unlock_prompt_store_credential_message)
            message += "\n\n" + resources.getString(R.string.device_unlock_keystore_warning)
        }
        if (keystoreWarning && deleteKeys) {
            message += "\n\n"
        }
        if (deleteKeys) {
            message += resources.getString(R.string.device_unlock_delete_all_key_warning)
        }
        warningAlertDialog = AlertDialog.Builder(activity)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(resources.getString(android.R.string.ok)
            ) { _, _ ->
                validate?.invoke()
                warningAlertDialog?.setOnDismissListener(null)
                if (deleteKeys && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    DeviceUnlockManager.deleteAllEntryKeysInKeystoreForBiometric(activity)
                }
            }
            .setNegativeButton(resources.getString(android.R.string.cancel)
            ) { _, _ ->}
            .create()
        warningAlertDialog?.show()
    }

    private fun onCreateAppearancePreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey)

        activity?.let { activity ->
            findPreference<ListPreference>(getString(R.string.setting_style_key))?.setOnPreferenceChangeListener { _, newValue ->
                var styleEnabled = true
                val styleIdString = newValue as String
                if (!activity.isContributingUser()) {
                    for (themeIdDisabled in BuildConfig.STYLES_DISABLED) {
                        if (themeIdDisabled == styleIdString) {
                            styleEnabled = false
                            ProFeatureDialogFragment().show(
                                parentFragmentManager,
                                "pro_feature_dialog"
                            )
                        }
                    }
                }
                if (styleEnabled) {
                    Stylish.assignStyle(activity, styleIdString)
                    // Relaunch the current activity to redraw theme
                    (activity as? SettingsActivity?)?.apply {
                        reloadActivity()
                    }
                }
                styleEnabled
            }

            findPreference<ListPreference>(getString(R.string.setting_style_brightness_key))?.setOnPreferenceChangeListener { _, _ ->
                (activity as? SettingsActivity?)?.apply {
                    reloadActivity()
                }
                true
            }

            findPreference<IconPackListPreference>(getString(R.string.setting_icon_pack_choose_key))?.setOnPreferenceChangeListener { _, newValue ->
                var iconPackEnabled = true
                val iconPackId = newValue as String
                if (!activity.isContributingUser()) {
                    for (iconPackIdDisabled in BuildConfig.ICON_PACKS_DISABLED) {
                        if (iconPackIdDisabled == iconPackId) {
                            iconPackEnabled = false
                            ProFeatureDialogFragment().show(
                                parentFragmentManager,
                                "pro_feature_dialog"
                            )
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

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // To reload group when appearance settings are modified
        when (preference.key) {
            getString(R.string.setting_style_key),
            getString(R.string.setting_style_brightness_key),
            getString(R.string.setting_icon_pack_choose_key),
            getString(R.string.show_entry_colors_key),
            getString(R.string.hide_expired_entries_key),
            getString(R.string.hide_templates_key),
            getString(R.string.list_entries_show_username_key),
            getString(R.string.list_groups_show_number_entries_key),
            getString(R.string.recursive_number_entries_key),
            getString(R.string.show_otp_token_key),
            getString(R.string.show_uuid_key),
            getString(R.string.list_size_key),
            getString(R.string.monospace_font_fields_enable_key),
            getString(R.string.enable_education_screens_key),
            getString(R.string.reset_education_screens_key) -> {
                DATABASE_PREFERENCE_CHANGED = true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.app_timeout_key),
            getString(R.string.clipboard_timeout_key),
            getString(R.string.temp_device_unlock_timeout_key) -> {
                dialogFragment = DurationDialogFragmentCompat.newInstance(preference.key)
            }
            else -> otherDialogFragment = true
        }

        if (dialogFragment != null) {
            @Suppress("DEPRECATION")
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, TAG_PREF_FRAGMENT)
        }
        // Could not be handled here. Try with the super method.
        else if (otherDialogFragment) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findPreference<TwoStatePreference?>(getString(R.string.settings_autofill_enable_key))?.let { autoFillEnablePreference ->
                    val autofillManager = activity.getSystemService(AutofillManager::class.java)
                    autoFillEnablePreference.isChecked = autofillManager != null
                            && autofillManager.hasEnabledAutofillServices()
                }
            }
        }
    }

    override fun onPause() {
        warningAlertDialog?.dismiss()
        super.onPause()
    }

    private var mCount = 0
    override fun onStop() {
        super.onStop()
        activity?.let { activity ->
            if (mCount == 10 && !BuildConfig.CLOSED_STORE) {
                Education.setEducationScreenReclickedPerformed(activity)
            }
        }
    }

    companion object {
        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"

        var DATABASE_PREFERENCE_CHANGED = false
    }
}