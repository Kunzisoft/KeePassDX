/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.hardware.fingerprint.FingerprintManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v14.preference.SwitchPreference
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceCategory
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import android.view.autofill.AutofillManager
import android.widget.Toast
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.KeyboardExplanationDialogFragment
import com.kunzisoft.keepass.activities.dialogs.ProFeatureDialogFragment
import com.kunzisoft.keepass.activities.dialogs.UnavailableFeatureDialogFragment
import com.kunzisoft.keepass.activities.dialogs.UnderDevelopmentFeatureDialogFragment
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.fileselect.database.FileDatabaseHistory
import com.kunzisoft.keepass.fingerprint.FingerPrintHelper
import com.kunzisoft.keepass.fingerprint.FingerPrintViewsManager
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.settings.preferencedialogfragment.*
import java.lang.ref.WeakReference

class NestedSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var database: Database = Database.getInstance()
    private var databaseReadOnly: Boolean = false

    private var count = 0

    private var roundPref: Preference? = null
    private var memoryPref: Preference? = null
    private var parallelismPref: Preference? = null

    enum class Screen {
        APPLICATION, FORM_FILLING, DATABASE, APPEARANCE
    }

    override fun onResume() {
        super.onResume()

        activity?.let { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autoFillEnablePreference = findPreference(getString(R.string.settings_autofill_enable_key)) as SwitchPreference?
                if (autoFillEnablePreference != null) {
                    val autofillManager = activity.getSystemService(AutofillManager::class.java)
                    autoFillEnablePreference.isChecked = autofillManager != null
                            && autofillManager.hasEnabledAutofillServices()
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        var key = 0
        if (arguments != null)
            key = arguments!!.getInt(TAG_KEY)

        databaseReadOnly = database.isReadOnly
                || ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, arguments)

        // Load the preferences from an XML resource
        when (Screen.values()[key]) {
            Screen.APPLICATION -> {
                onCreateApplicationPreferences(rootKey)
            }
            Screen.FORM_FILLING -> {
                onCreateFormFillingPreference(rootKey)
            }
            Screen.APPEARANCE -> {
                onCreateAppearancePreferences(rootKey)
            }
            Screen.DATABASE -> {
                onCreateDatabasePreference(rootKey)
            }
        }
    }

    private fun onCreateApplicationPreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.application_preferences, rootKey)

        activity?.let { activity ->
            allowCopyPassword()

            val keyFile = findPreference(getString(R.string.keyfile_key))
            keyFile.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistory.getInstance(WeakReference(activity.applicationContext)).deleteAllKeys()
                }
                true
            }

            val recentHistory = findPreference(getString(R.string.recentfile_key))
            recentHistory.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean)) {
                    FileDatabaseHistory.getInstance(WeakReference(activity.applicationContext)).deleteAll()
                }
                true
            }

            val storageAccessFramework = findPreference(getString(R.string.saf_key)) as SwitchPreference
            storageAccessFramework.setOnPreferenceChangeListener { _, newValue ->
                if (!(newValue as Boolean) && context != null) {
                    val alertDialog = AlertDialog.Builder(context!!)
                            .setMessage(getString(R.string.warning_disabling_storage_access_framework)).create()
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getText(android.R.string.ok)
                    ) { dialog, _ -> dialog.dismiss() }
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getText(android.R.string.cancel)
                    ) { dialog, _ ->
                        storageAccessFramework.isChecked = true
                        dialog.dismiss()
                    }
                    alertDialog.show()
                }
                true
            }

            val fingerprintEnablePreference = findPreference(getString(R.string.fingerprint_enable_key)) as SwitchPreference
            // < M solve verifyError exception
            var fingerprintSupported = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                fingerprintSupported = FingerPrintHelper.isFingerprintSupported(
                        activity.getSystemService(FingerprintManager::class.java))
            if (!fingerprintSupported) {
                // False if under Marshmallow
                fingerprintEnablePreference.isChecked = false
                fingerprintEnablePreference.setOnPreferenceClickListener { preference ->
                    fragmentManager?.let { fragmentManager ->
                        (preference as SwitchPreference).isChecked = false
                        UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.M)
                                .show(fragmentManager, "unavailableFeatureDialog")
                    }
                    false
                }
            }

            val deleteKeysFingerprints = findPreference(getString(R.string.fingerprint_delete_all_key))
            if (!fingerprintSupported) {
                deleteKeysFingerprints.isEnabled = false
            } else {
                deleteKeysFingerprints.setOnPreferenceClickListener {
                    context?.let { context ->
                        AlertDialog.Builder(context)
                                .setMessage(resources.getString(R.string.fingerprint_delete_all_warning))
                                .setIcon(resources.getDrawable(
                                        android.R.drawable.ic_dialog_alert))
                                .setPositiveButton(resources.getString(android.R.string.yes)
                                ) { _, _ ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        FingerPrintHelper.deleteEntryKeyInKeystoreForFingerprints(
                                                context,
                                                object : FingerPrintHelper.FingerPrintErrorCallback {
                                                    override fun onInvalidKeyException(e: Exception) {}

                                                    override fun onFingerPrintException(e: Exception) {
                                                        Toast.makeText(context,
                                                                getString(R.string.fingerprint_error, e.localizedMessage),
                                                                Toast.LENGTH_SHORT).show()
                                                    }
                                                })
                                    }
                                    FingerPrintViewsManager.deleteAllValuesFromNoBackupPreferences(context)
                                }
                                .setNegativeButton(resources.getString(android.R.string.no))
                                { _, _ -> }.show()
                    }
                    false
                }
            }
        }
    }

    private fun onCreateFormFillingPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.form_filling_preferences, rootKey)

        activity?.let { activity ->
            val autoFillEnablePreference = findPreference(getString(R.string.settings_autofill_enable_key)) as SwitchPreference
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val autofillManager = activity.getSystemService(AutofillManager::class.java)
                if (autofillManager != null && autofillManager.hasEnabledAutofillServices())
                    autoFillEnablePreference.isChecked = autofillManager.hasEnabledAutofillServices()
                autoFillEnablePreference.onPreferenceClickListener = object : Preference.OnPreferenceClickListener {
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
                            intent.data = Uri.parse("package:com.example.android.autofill.service")
                            Log.d(javaClass.name, "enableService(): intent=$intent")
                            startActivityForResult(intent, REQUEST_CODE_AUTOFILL)
                        } else {
                            Log.d(javaClass.name, "Sample service already enabled.")
                        }
                    }
                }
            } else {
                autoFillEnablePreference.setOnPreferenceClickListener { preference ->
                    (preference as SwitchPreference).isChecked = false
                    val fragmentManager = fragmentManager!!
                    UnavailableFeatureDialogFragment.getInstance(Build.VERSION_CODES.O)
                            .show(fragmentManager, "unavailableFeatureDialog")
                    false
                }
            }
        }

        val keyboardPreference = findPreference(getString(R.string.magic_keyboard_key))
        keyboardPreference.setOnPreferenceClickListener {
            if (fragmentManager != null) {
                KeyboardExplanationDialogFragment().show(fragmentManager!!, "keyboardExplanationDialog")
            }
            false
        }

        val keyboardSubPreference = findPreference(getString(R.string.magic_keyboard_preference_key))
        keyboardSubPreference.setOnPreferenceClickListener {
            startActivity(Intent(context, MagikIMESettings::class.java))
            false
        }

        // Present in two places
        allowCopyPassword()
    }

    private fun onCreateAppearancePreferences(rootKey: String?) {
        setPreferencesFromResource(R.xml.appearance_preferences, rootKey)

        activity?.let { activity ->
            val stylePreference = findPreference(getString(R.string.setting_style_key))
            stylePreference.setOnPreferenceChangeListener { _, newValue ->
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

            val iconPackPreference = findPreference(getString(R.string.setting_icon_pack_choose_key))
            iconPackPreference.setOnPreferenceChangeListener { _, newValue ->
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

            val resetEducationScreens = findPreference(getString(R.string.reset_education_screens_key))
            resetEducationScreens.setOnPreferenceClickListener {
                // To allow only one toast
                if (count == 0) {
                    val sharedPreferences = Education.getEducationSharedPreferences(context!!)
                    val editor = sharedPreferences.edit()
                    for (resourceId in Education.educationResourcesKeys) {
                        editor.putBoolean(getString(resourceId), false)
                    }
                    editor.apply()
                    Toast.makeText(context, R.string.reset_education_screens_text, Toast.LENGTH_SHORT).show()
                }
                count++
                false
            }
        }
    }

    private fun onCreateDatabasePreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.database_preferences, rootKey)

        if (database.loaded) {

            val dbGeneralPrefCategory = findPreference(getString(R.string.database_general_key)) as PreferenceCategory

            // Db name
            val dbNamePref = findPreference(getString(R.string.database_name_key))
            if (database.containsName()) {
                dbNamePref.summary = database.name
            } else {
                dbGeneralPrefCategory.removePreference(dbNamePref)
            }

            // Db description
            val dbDescriptionPref = findPreference(getString(R.string.database_description_key))
            if (database.containsDescription()) {
                dbDescriptionPref.summary = database.description
            } else {
                dbGeneralPrefCategory.removePreference(dbDescriptionPref)
            }

            // Recycle bin
            val recycleBinPref = findPreference(getString(R.string.recycle_bin_key)) as SwitchPreference
            // TODO Recycle
            dbGeneralPrefCategory.removePreference(recycleBinPref) // To delete
            if (database.isRecycleBinAvailable) {
                recycleBinPref.isChecked = database.isRecycleBinEnabled
                recycleBinPref.isEnabled = false
            } else {
                dbGeneralPrefCategory.removePreference(recycleBinPref)
            }

            // Version
            val dbVersionPref = findPreference(getString(R.string.database_version_key))
            dbVersionPref.summary = database.getVersion()

            // Encryption Algorithm
            val algorithmPref = findPreference(getString(R.string.encryption_algorithm_key))
            algorithmPref.summary = database.getEncryptionAlgorithmName(resources)

            // Key derivation function
            val kdfPref = findPreference(getString(R.string.key_derivation_function_key))
            kdfPref.summary = database.getKeyDerivationName(resources)

            // Round encryption
            roundPref = findPreference(getString(R.string.transform_rounds_key))
            roundPref?.summary = database.numberKeyEncryptionRoundsAsString

            // Memory Usage
            memoryPref = findPreference(getString(R.string.memory_usage_key))
            memoryPref?.summary = database.memoryUsageAsString

            // Parallelism
            parallelismPref = findPreference(getString(R.string.parallelism_key))
            parallelismPref?.summary = database.parallelismAsString

        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
    }

    private fun allowCopyPassword() {
        val copyPasswordPreference = findPreference(getString(R.string.allow_copy_password_key)) as SwitchPreference
        copyPasswordPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean && context != null) {
                val message = getString(R.string.allow_copy_password_warning) +
                        "\n\n" +
                        getString(R.string.clipboard_warning)
                AlertDialog
                        .Builder(context!!)
                        .setMessage(message)
                        .create()
                        .apply {
                            setButton(AlertDialog.BUTTON_POSITIVE, getText(android.R.string.ok))
                            { dialog, _ ->
                                dialog.dismiss()
                            }
                            setButton(AlertDialog.BUTTON_NEGATIVE, getText(android.R.string.cancel))
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

    private fun preferenceInDevelopment(preferenceInDev: Preference) {
        preferenceInDev.setOnPreferenceClickListener { preference ->
            fragmentManager?.let { fragmentManager ->
                try { // don't check if we can
                    (preference as SwitchPreference).isChecked = false
                } catch (ignored: Exception) {
                }
                UnderDevelopmentFeatureDialogFragment().show(fragmentManager, "underDevFeatureDialog")
            }
            false
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.let { activity ->
            if (count == 10) {
                Education.getEducationSharedPreferences(activity).edit()
                        .putBoolean(getString(R.string.education_screen_reclicked_key), true).apply()
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {

        var otherDialogFragment = false

        fragmentManager?.let { fragmentManager ->
            preference?.let { preference ->
                var dialogFragment: DialogFragment? = null
                when {
                    preference.key == getString(R.string.database_name_key) -> {
                        dialogFragment = DatabaseNamePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.database_description_key) -> {
                        dialogFragment = DatabaseDescriptionPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.encryption_algorithm_key) -> {
                        dialogFragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.key_derivation_function_key) -> {
                        val keyDerivationDialogFragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat.newInstance(preference.key)
                        // Add other prefs to manage
                        if (roundPref != null)
                            keyDerivationDialogFragment.setRoundPreference(roundPref!!)
                        if (memoryPref != null)
                            keyDerivationDialogFragment.setMemoryPreference(memoryPref!!)
                        if (parallelismPref != null)
                            keyDerivationDialogFragment.setParallelismPreference(parallelismPref!!)
                        dialogFragment = keyDerivationDialogFragment
                    }
                    preference.key == getString(R.string.transform_rounds_key) -> {
                        dialogFragment = RoundsPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.memory_usage_key) -> {
                        dialogFragment = MemoryUsagePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.parallelism_key) -> {
                        dialogFragment = ParallelismPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    else -> otherDialogFragment = true
                }

                if (dialogFragment != null && !databaseReadOnly) {
                    dialogFragment.setTargetFragment(this, 0)
                    dialogFragment.show(fragmentManager, null)
                }
                // Could not be handled here. Try with the super method.
                else if (otherDialogFragment) {
                    super.onDisplayPreferenceDialog(preference)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        ReadOnlyHelper.onSaveInstanceState(outState, databaseReadOnly)
        super.onSaveInstanceState(outState)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        // TODO encapsulate
        return false
    }

    companion object {

        private const val TAG_KEY = "NESTED_KEY"

        private const val REQUEST_CODE_AUTOFILL = 5201

        @JvmOverloads
        fun newInstance(key: Screen, databaseReadOnly: Boolean = ReadOnlyHelper.READ_ONLY_DEFAULT)
                : NestedSettingsFragment {
            val fragment = NestedSettingsFragment()
            // supply arguments to bundle.
            val args = Bundle()
            args.putInt(TAG_KEY, key.ordinal)
            ReadOnlyHelper.putReadOnlyInBundle(args, databaseReadOnly)
            fragment.arguments = args
            return fragment
        }

        fun retrieveTitle(resources: Resources, key: Screen): String {
            return when (key) {
                Screen.APPLICATION -> resources.getString(R.string.menu_app_settings)
                Screen.FORM_FILLING -> resources.getString(R.string.menu_form_filling_settings)
                Screen.DATABASE -> resources.getString(R.string.menu_db_settings)
                Screen.APPEARANCE -> resources.getString(R.string.appearance)
            }
        }
    }
}
