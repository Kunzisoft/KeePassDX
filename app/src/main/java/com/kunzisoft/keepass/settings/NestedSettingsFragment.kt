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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.kunzisoft.androidclearchroma.ChromaUtil
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.*
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.app.database.FileDatabaseHistoryAction
import com.kunzisoft.keepass.biometric.BiometricUnlockDatabaseHelper
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.PwCompressionAlgorithm
import com.kunzisoft.keepass.database.element.PwEncryptionAlgorithm
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.icons.IconPackChooser
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_COLOR_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_COMPRESSION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_DEFAULT_USERNAME_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_DESCRIPTION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_ENCRYPTION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_ITERATIONS_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_KEY_DERIVATION_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_MAX_HISTORY_ITEMS_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_MAX_HISTORY_SIZE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_MEMORY_USAGE_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_NAME_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_SAVE_PARALLELISM_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.NEW_ELEMENT_KEY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.OLD_ELEMENT_KEY
import com.kunzisoft.keepass.settings.preference.*
import com.kunzisoft.keepass.settings.preference.DialogColorPreference.Companion.DISABLE_COLOR
import com.kunzisoft.keepass.settings.preferencedialogfragment.*
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.UriUtil

class NestedSettingsFragment : PreferenceFragmentCompat() {

    private var mDatabase: Database = Database.getInstance()
    private var mDatabaseReadOnly: Boolean = false

    private var mCount = 0

    private var dbNamePref: InputTextPreference? = null
    private var dbDescriptionPref: InputTextPreference? = null
    private var dbDefaultUsername: InputTextPreference? = null
    private var dbCustomColorPref: DialogColorPreference? = null
    private var dbDataCompressionPref: Preference? = null
    private var recycleBinGroupPref: Preference? = null
    private var dbMaxHistoryItemsPref: InputNumberPreference? = null
    private var dbMaxHistorySizePref: InputNumberPreference? = null
    private var mEncryptionAlgorithmPref: DialogListExplanationPreference? = null
    private var mKeyDerivationPref: DialogListExplanationPreference? = null
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
                onCreateAdvancedUnlockPreferences(rootKey)
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

    private fun onCreateDatabasePreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database, rootKey)

        if (mDatabase.loaded) {

            val dbGeneralPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_general_key))

            // Database name
            dbNamePref = findPreference(getString(R.string.database_name_key))
            if (mDatabase.allowName) {
                dbNamePref?.summary = mDatabase.name
            } else {
                dbGeneralPrefCategory?.removePreference(dbNamePref)
            }

            // Database description
            dbDescriptionPref = findPreference(getString(R.string.database_description_key))
            if (mDatabase.allowDescription) {
                dbDescriptionPref?.summary = mDatabase.description
            } else {
                dbGeneralPrefCategory?.removePreference(dbDescriptionPref)
            }

            // Database default username
            dbDefaultUsername = findPreference(getString(R.string.database_default_username_key))
            if (mDatabase.allowDefaultUsername) {
                dbDefaultUsername?.summary = mDatabase.defaultUsername
            } else {
                dbDefaultUsername?.isEnabled = false
                // TODO dbGeneralPrefCategory?.removePreference(dbDefaultUsername)
            }

            // Database custom color
            dbCustomColorPref = findPreference(getString(R.string.database_custom_color_key))
            if (mDatabase.allowCustomColor) {
                dbCustomColorPref?.apply {
                    try {
                        color = Color.parseColor(mDatabase.customColor)
                        summary = mDatabase.customColor
                    } catch (e: Exception) {
                        color = DISABLE_COLOR
                        summary = ""
                    }
                }
            } else {
                dbCustomColorPref?.isEnabled = false
                // TODO dbGeneralPrefCategory?.removePreference(dbCustomColorPref)
            }

            // Version
            findPreference<Preference>(getString(R.string.database_version_key))
                    ?.summary = mDatabase.version

            val dbCompressionPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_compression_key))

            // Database compression
            dbDataCompressionPref = findPreference(getString(R.string.database_data_compression_key))
            if (mDatabase.allowDataCompression) {
                dbDataCompressionPref?.summary = (mDatabase.compressionAlgorithm
                        ?: PwCompressionAlgorithm.None).getName(resources)
            } else {
                dbCompressionPrefCategory?.isVisible = false
            }

            val dbRecycleBinPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_recycle_bin_key))
            recycleBinGroupPref = findPreference(getString(R.string.recycle_bin_group_key))

            // Recycle bin
            if (mDatabase.allowRecycleBin) {
                val recycleBinEnablePref: SwitchPreference? = findPreference(getString(R.string.recycle_bin_enable_key))
                recycleBinEnablePref?.apply {
                    isChecked = mDatabase.isRecycleBinEnabled
                    isEnabled = if (!mDatabaseReadOnly) {
                        setOnPreferenceChangeListener { _, newValue ->
                            val recycleBinEnabled = newValue as Boolean
                            mDatabase.isRecycleBinEnabled = recycleBinEnabled
                            if (recycleBinEnabled) {
                                mDatabase.ensureRecycleBinExists(resources)
                            } else {
                                mDatabase.removeRecycleBin()
                            }
                            refreshRecycleBinGroup()
                            // Save the database if not in readonly mode
                            (context as SettingsActivity?)?.progressDialogThread?.startDatabaseSave(true)
                            true
                        }
                        true
                    } else {
                        false
                    }
                }
                // Recycle Bin group
                refreshRecycleBinGroup()
            } else {
                dbRecycleBinPrefCategory?.isVisible = false
            }

            // History
            findPreference<PreferenceCategory>(getString(R.string.database_category_history_key))
                    ?.isVisible = mDatabase.manageHistory == true

            // Max history items
            dbMaxHistoryItemsPref = findPreference<InputNumberPreference>(getString(R.string.max_history_items_key))?.apply {
                summary = mDatabase.historyMaxItems.toString()
            }

            // Max history size
            dbMaxHistorySizePref = findPreference<InputNumberPreference>(getString(R.string.max_history_size_key))?.apply {
                summary = mDatabase.historyMaxSize.toString()
            }

        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
    }

    private fun refreshRecycleBinGroup() {
        recycleBinGroupPref?.apply {
            if (mDatabase.isRecycleBinEnabled) {
                summary = mDatabase.recycleBin?.title
                isEnabled = true
            } else {
                summary = null
                isEnabled = false
            }
        }
    }

    private fun onCreateDatabaseSecurityPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database_security, rootKey)

        if (mDatabase.loaded) {
            // Encryption Algorithm
            mEncryptionAlgorithmPref = findPreference<DialogListExplanationPreference>(getString(R.string.encryption_algorithm_key))?.apply {
                summary = mDatabase.getEncryptionAlgorithmName(resources)
            }

            // Key derivation function
            mKeyDerivationPref = findPreference<DialogListExplanationPreference>(getString(R.string.key_derivation_function_key))?.apply {
                summary = mDatabase.getKeyDerivationName(resources)
            }

            // Round encryption
            mRoundPref = findPreference<InputKdfNumberPreference>(getString(R.string.transform_rounds_key))?.apply {
                summary = mDatabase.numberKeyEncryptionRounds.toString()
            }

            // Memory Usage
            mMemoryPref = findPreference<InputKdfNumberPreference>(getString(R.string.memory_usage_key))?.apply {
                summary = mDatabase.memoryUsage.toString()
            }

            // Parallelism
            mParallelismPref = findPreference<InputKdfNumberPreference>(getString(R.string.parallelism_key))?.apply {
                summary = mDatabase.parallelism.toString()
            }
        } else {
            Log.e(javaClass.name, "Database isn't ready")
        }
    }

    private fun onCreateDatabaseMasterKeyPreference(rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_database_master_key, rootKey)

        if (mDatabase.loaded) {
            findPreference<Preference>(getString(R.string.settings_database_change_credentials_key))?.apply {
                isEnabled = if (!mDatabaseReadOnly) {
                    onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        fragmentManager?.let { fragmentManager ->
                            AssignMasterKeyDialogFragment.getInstance(mDatabase.allowNoMasterKey)
                                    .show(fragmentManager, "passwordDialog")
                        }
                        false
                    }
                    true
                } else {
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

    private val colorSelectedListener: ((Boolean, Int)-> Unit)? = { enable, color ->
        dbCustomColorPref?.summary = ChromaUtil.getFormattedColorString(color, false)
        if (enable) {
            dbCustomColorPref?.color = color
        } else {
            dbCustomColorPref?.color = DISABLE_COLOR
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        try {
            // To reassign color listener after orientation change
            val chromaDialog = fragmentManager?.findFragmentByTag(TAG_PREF_FRAGMENT) as DatabaseColorPreferenceDialogFragmentCompat?
            chromaDialog?.onColorSelectedListener = colorSelectedListener
        } catch (e: Exception) {}

        return view
    }

    fun onProgressDialogThreadResult(actionTask: String,
                                     result: ActionRunnable.Result) {
        result.data?.let { data ->
            if (data.containsKey(OLD_ELEMENT_KEY)
                    && data.containsKey(NEW_ELEMENT_KEY)) {
                when (actionTask) {
                    /*
                    --------
                    Main preferences
                    --------
                    */
                    ACTION_DATABASE_SAVE_NAME_TASK -> {
                        val oldName = data.getString(OLD_ELEMENT_KEY)!!
                        val newName = data.getString(NEW_ELEMENT_KEY)!!
                        val nameToShow =
                                if (result.isSuccess) {
                                    newName
                                } else {
                                    mDatabase.name = oldName
                                    oldName
                                }
                        dbNamePref?.summary = nameToShow
                    }
                    ACTION_DATABASE_SAVE_DESCRIPTION_TASK -> {
                        val oldDescription = data.getString(OLD_ELEMENT_KEY)!!
                        val newDescription = data.getString(NEW_ELEMENT_KEY)!!
                        val descriptionToShow =
                                if (result.isSuccess) {
                                    newDescription
                                } else {
                                    mDatabase.description = oldDescription
                                    oldDescription
                                }
                        dbDescriptionPref?.summary = descriptionToShow
                    }
                    ACTION_DATABASE_SAVE_DEFAULT_USERNAME_TASK -> {
                        val oldDefaultUsername = data.getString(OLD_ELEMENT_KEY)!!
                        val newDefaultUsername = data.getString(NEW_ELEMENT_KEY)!!
                        val defaultUsernameToShow =
                                if (result.isSuccess) {
                                    newDefaultUsername
                                } else {
                                    mDatabase.defaultUsername = oldDefaultUsername
                                    oldDefaultUsername
                                }
                        dbDefaultUsername?.summary = defaultUsernameToShow
                    }
                    ACTION_DATABASE_SAVE_COLOR_TASK -> {
                        val oldColor = data.getString(OLD_ELEMENT_KEY)!!
                        val newColor = data.getString(NEW_ELEMENT_KEY)!!

                        val defaultColorToShow =
                                if (result.isSuccess) {
                                    newColor
                                } else {
                                    mDatabase.customColor = oldColor
                                    oldColor
                                }
                        dbCustomColorPref?.summary = defaultColorToShow
                    }
                    ACTION_DATABASE_SAVE_COMPRESSION_TASK -> {
                        val oldCompression = data.getSerializable(OLD_ELEMENT_KEY) as PwCompressionAlgorithm
                        val newCompression = data.getSerializable(NEW_ELEMENT_KEY) as PwCompressionAlgorithm
                        val algorithmToShow =
                                if (result.isSuccess) {
                                    newCompression
                                } else {
                                    mDatabase.compressionAlgorithm = oldCompression
                                    oldCompression
                                }
                        dbDataCompressionPref?.summary = algorithmToShow.getName(resources)
                    }
                    ACTION_DATABASE_SAVE_MAX_HISTORY_ITEMS_TASK -> {
                        val oldMaxHistoryItems = data.getInt(OLD_ELEMENT_KEY)
                        val newMaxHistoryItems = data.getInt(NEW_ELEMENT_KEY)
                        val maxHistoryItemsToShow =
                                if (result.isSuccess) {
                                    newMaxHistoryItems
                                } else {
                                    mDatabase.historyMaxItems = oldMaxHistoryItems
                                    oldMaxHistoryItems
                                }
                        dbMaxHistoryItemsPref?.summary = maxHistoryItemsToShow.toString()
                    }
                    ACTION_DATABASE_SAVE_MAX_HISTORY_SIZE_TASK -> {
                        val oldMaxHistorySize = data.getLong(OLD_ELEMENT_KEY)
                        val newMaxHistorySize = data.getLong(NEW_ELEMENT_KEY)
                        val maxHistorySizeToShow =
                                if (result.isSuccess) {
                                    newMaxHistorySize
                                } else {
                                    mDatabase.historyMaxSize = oldMaxHistorySize
                                    oldMaxHistorySize
                                }
                        dbMaxHistorySizePref?.summary = maxHistorySizeToShow.toString()
                    }

                    /*
                    --------
                    Security
                    --------
                     */
                    ACTION_DATABASE_SAVE_ENCRYPTION_TASK -> {
                        val oldEncryption = data.getSerializable(OLD_ELEMENT_KEY) as PwEncryptionAlgorithm
                        val newEncryption = data.getSerializable(NEW_ELEMENT_KEY) as PwEncryptionAlgorithm
                        val algorithmToShow =
                                if (result.isSuccess) {
                                    newEncryption
                                } else {
                                    mDatabase.encryptionAlgorithm = oldEncryption
                                    oldEncryption
                                }
                        mEncryptionAlgorithmPref?.summary = algorithmToShow.getName(resources)
                    }
                    ACTION_DATABASE_SAVE_KEY_DERIVATION_TASK -> {
                        val oldKeyDerivationEngine = data.getSerializable(OLD_ELEMENT_KEY) as KdfEngine
                        val newKeyDerivationEngine = data.getSerializable(NEW_ELEMENT_KEY) as KdfEngine
                        val kdfEngineToShow =
                                if (result.isSuccess) {
                                    newKeyDerivationEngine
                                } else {
                                    mDatabase.kdfEngine = oldKeyDerivationEngine
                                    oldKeyDerivationEngine
                                }
                        mKeyDerivationPref?.summary = kdfEngineToShow.getName(resources)

                        mRoundPref?.summary = kdfEngineToShow.defaultKeyRounds.toString()
                        // Disable memory and parallelism if not available
                        mMemoryPref?.summary = kdfEngineToShow.defaultMemoryUsage.toString()
                        mParallelismPref?.summary = kdfEngineToShow.defaultParallelism.toString()
                    }
                    ACTION_DATABASE_SAVE_ITERATIONS_TASK -> {
                        val oldIterations = data.getLong(OLD_ELEMENT_KEY)
                        val newIterations = data.getLong(NEW_ELEMENT_KEY)
                        val roundsToShow =
                                if (result.isSuccess) {
                                    newIterations
                                } else {
                                    mDatabase.numberKeyEncryptionRounds = oldIterations
                                    oldIterations
                                }
                        mRoundPref?.summary = roundsToShow.toString()
                    }
                    ACTION_DATABASE_SAVE_MEMORY_USAGE_TASK -> {
                        val oldMemoryUsage = data.getLong(OLD_ELEMENT_KEY)
                        val newMemoryUsage = data.getLong(NEW_ELEMENT_KEY)
                        val memoryToShow =
                                if (result.isSuccess) {
                                    newMemoryUsage
                                } else {
                                    mDatabase.memoryUsage = oldMemoryUsage
                                    oldMemoryUsage
                                }
                        mMemoryPref?.summary = memoryToShow.toString()
                    }
                    ACTION_DATABASE_SAVE_PARALLELISM_TASK -> {
                        val oldParallelism = data.getInt(OLD_ELEMENT_KEY)
                        val newParallelism = data.getInt(NEW_ELEMENT_KEY)
                        val parallelismToShow =
                                if (result.isSuccess) {
                                    newParallelism
                                } else {
                                    mDatabase.parallelism = oldParallelism
                                    oldParallelism
                                }
                        mParallelismPref?.summary = parallelismToShow.toString()
                    }
                }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {

        var otherDialogFragment = false

        fragmentManager?.let { fragmentManager ->
            preference?.let { preference ->
                var dialogFragment: DialogFragment? = null
                when {
                    // Main Preferences
                    preference.key == getString(R.string.database_name_key) -> {
                        dialogFragment = DatabaseNamePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.database_description_key) -> {
                        dialogFragment = DatabaseDescriptionPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.database_default_username_key) -> {
                        dialogFragment = DatabaseDefaultUsernamePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.database_custom_color_key) -> {
                        dialogFragment = DatabaseColorPreferenceDialogFragmentCompat.newInstance(preference.key).apply {
                            onColorSelectedListener = colorSelectedListener
                        }
                    }
                    preference.key == getString(R.string.database_data_compression_key) -> {
                        dialogFragment = DatabaseDataCompressionPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.max_history_items_key) -> {
                        dialogFragment = MaxHistoryItemsPreferenceDialogFragmentCompat.newInstance(preference.key)
                    }
                    preference.key == getString(R.string.max_history_size_key) -> {
                        dialogFragment = MaxHistorySizePreferenceDialogFragmentCompat.newInstance(preference.key)
                    }

                    // Security
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
                    dialogFragment.show(fragmentManager, TAG_PREF_FRAGMENT)
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

    companion object {

        private const val TAG_KEY = "NESTED_KEY"

        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"

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
