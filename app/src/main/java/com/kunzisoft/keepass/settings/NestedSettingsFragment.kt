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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.preference.*
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.*
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.biometric.BiometricUnlockDatabaseHelper
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.settings.preference.*
import com.kunzisoft.keepass.settings.preferencedialogfragment.*

class NestedSettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private var mDatabase: Database = Database.getInstance()
    private var mDatabaseReadOnly: Boolean = false

    private var mCount = 0

    private var mRoundPref: InputKdfNumberPreference? = null
    private var mMemoryPref: InputKdfNumberPreference? = null
    private var mParallelismPref: InputKdfNumberPreference? = null

    enum class Screen {
        APPLICATION, FORM_FILLING, ADVANCED_UNLOCK, APPEARANCE, DATABASE, DATABASE_SECURITY, DATABASE_MASTER_KEY
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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        var key = 0
        if (arguments != null)
            key = arguments!!.getInt(TAG_KEY)

        mDatabaseReadOnly = mDatabase.isReadOnly
                || ReadOnlyHelper.retrieveReadOnlyFromInstanceStateOrArguments(savedInstanceState, arguments)

        // Load the preferences from an XML resource
        when (Screen.values()[key]) {
            Screen.APPLICATION -> {
                onCreateApplicationPreferences(rootKey)
            }
            Screen.FORM_FILLING -> {
                onCreateFormFillingPreference(rootKey)
            }
            Screen.ADVANCED_UNLOCK -> {
                onCreateAdvancesUnlockPreferences(rootKey)
            }
            Screen.APPEARANCE -> {
                onCreateAppearancePreferences(rootKey)
            }
            Screen.DATABASE -> {
                onCreateDatabasePreference(rootKey)
            }
            Screen.DATABASE_SECURITY -> {
                onCreateDatabaseSecurityPreference(rootKey)
            }
            Screen.DATABASE_MASTER_KEY -> {
                onCreateDatabaseMasterKeyPreference(rootKey)
            }
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

        findPreference<Preference>(getString(R.string.magic_keyboard_key))?.setOnPreferenceClickListener {
            if (fragmentManager != null) {
                KeyboardExplanationDialogFragment().show(fragmentManager!!, "keyboardExplanationDialog")
            }
            false
        }

        findPreference<Preference>(getString(R.string.magic_keyboard_preference_key))?.setOnPreferenceClickListener {
            startActivity(Intent(context, MagikIMESettings::class.java))
            false
        }

        // Present in two places
        allowCopyPassword()
    }

    private fun onCreateAdvancesUnlockPreferences(rootKey: String?) {
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

    private fun onCreateDatabasePreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database, rootKey)

        if (mDatabase.loaded) {

            val dbGeneralPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_general_key))

            // Db name
            val dbNamePref: InputTextPreference? = findPreference(getString(R.string.database_name_key))
            if (mDatabase.containsName()) {
                dbNamePref?.summary = mDatabase.name
            } else {
                dbGeneralPrefCategory?.removePreference(dbNamePref)
            }

            // Db description
            val dbDescriptionPref: InputTextPreference? = findPreference(getString(R.string.database_description_key))
            if (mDatabase.containsDescription()) {
                dbDescriptionPref?.summary = mDatabase.description
            } else {
                dbGeneralPrefCategory?.removePreference(dbDescriptionPref)
            }

            // Recycle bin
            val recycleBinPref: SwitchPreference? = findPreference(getString(R.string.recycle_bin_key))
            // TODO Recycle
            dbGeneralPrefCategory?.removePreference(recycleBinPref) // To delete
            if (mDatabase.isRecycleBinAvailable) {
                recycleBinPref?.isChecked = mDatabase.isRecycleBinEnabled
                recycleBinPref?.isEnabled = false
            } else {
                dbGeneralPrefCategory?.removePreference(recycleBinPref)
            }

            // Version
            findPreference<Preference>(getString(R.string.database_version_key))
                    ?.summary = mDatabase.getVersion()

            findPreference<PreferenceCategory>(getString(R.string.database_history_key))
                    ?.isVisible = mDatabase.manageHistory == true

            // Max history items
            findPreference<InputNumberPreference>(getString(R.string.max_history_items_key))
                    ?.summary = mDatabase.historyMaxItems.toString()

            // Max history size
            findPreference<InputNumberPreference>(getString(R.string.max_history_size_key))
                    ?.summary = mDatabase.historyMaxSize.toString()

        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
    }

    private fun onCreateDatabaseSecurityPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database_security, rootKey)

        if (mDatabase.loaded) {
            // Encryption Algorithm
            findPreference<DialogListExplanationPreference>(getString(R.string.encryption_algorithm_key))
                    ?.summary = mDatabase.getEncryptionAlgorithmName(resources)

            // Key derivation function
            findPreference<DialogListExplanationPreference>(getString(R.string.key_derivation_function_key))
                    ?.summary = mDatabase.getKeyDerivationName(resources)

            // Round encryption
            mRoundPref = findPreference(getString(R.string.transform_rounds_key))
            mRoundPref?.summary = mDatabase.numberKeyEncryptionRounds.toString()

            // Memory Usage
            mMemoryPref = findPreference(getString(R.string.memory_usage_key))
            mMemoryPref?.summary = mDatabase.memoryUsage.toString()

            // Parallelism
            mParallelismPref = findPreference(getString(R.string.parallelism_key))
            mParallelismPref?.summary = mDatabase.parallelism.toString()
        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
    }

    private fun onCreateDatabaseMasterKeyPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database_master_key, rootKey)

        if (mDatabase.loaded) {
            findPreference<Preference>(getString(R.string.settings_database_change_credentials_key))?.apply {
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    fragmentManager?.let { fragmentManager ->
                        AssignMasterKeyDialogFragment().show(fragmentManager, "passwordDialog")
                    }
                    false
                }
            }
        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
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
            if (mCount == 10) {
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
                    preference.key == getString(R.string.max_history_items_key) -> {
                        dialogFragment = MaxHistoryItemsPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.max_history_size_key) -> {
                        dialogFragment = MaxHistorySizePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.encryption_algorithm_key) -> {
                        dialogFragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.key_derivation_function_key) -> {
                        val keyDerivationDialogFragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat.newInstance(preference.key)
                        // Add other prefs to manage
                        keyDerivationDialogFragment.setRoundPreference(mRoundPref)
                        keyDerivationDialogFragment.setMemoryPreference(mMemoryPref)
                        keyDerivationDialogFragment.setParallelismPreference(mParallelismPref)
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

                if (dialogFragment != null && !mDatabaseReadOnly) {
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
        ReadOnlyHelper.onSaveInstanceState(outState, mDatabaseReadOnly)
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
                Screen.ADVANCED_UNLOCK -> resources.getString(R.string.menu_advanced_unlock_settings)
                Screen.APPEARANCE -> resources.getString(R.string.menu_appearance_settings)
                Screen.DATABASE -> resources.getString(R.string.menu_database_settings)
                Screen.DATABASE_SECURITY -> resources.getString(R.string.menu_security_settings)
                Screen.DATABASE_MASTER_KEY -> resources.getString(R.string.menu_master_key_settings)
            }
        }
    }
}
