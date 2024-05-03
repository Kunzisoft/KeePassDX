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

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.TwoStatePreference
import com.kunzisoft.androidclearchroma.ChromaUtil
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SetMainCredentialDialogFragment
import com.kunzisoft.keepass.activities.legacy.DatabaseRetrieval
import com.kunzisoft.keepass.activities.legacy.resetAppTimeoutWhenViewTouchedOrFocused
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.helper.*
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.settings.preference.*
import com.kunzisoft.keepass.settings.preferencedialogfragment.*
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getSerializableCompat
import com.kunzisoft.keepass.viewmodels.DatabaseViewModel

class NestedDatabaseSettingsFragment : NestedSettingsFragment(), DatabaseRetrieval {

    private val mDatabaseViewModel: DatabaseViewModel by activityViewModels()
    private var mDatabase: ContextualDatabase? = null
    private var mDatabaseReadOnly: Boolean = false
    private var mMergeDataAllowed: Boolean = false
    private var mDatabaseAutoSaveEnabled: Boolean = true

    private var mScreen: Screen? = null

    private var dbNamePref: InputTextPreference? = null
    private var dbDescriptionPref: InputTextPreference? = null
    private var dbDefaultUsernamePref: InputTextPreference? = null
    private var dbCustomColorPref: DialogColorPreference? = null
    private var dbDataCompressionPref: Preference? = null
    private var recycleBinGroupPref: DialogListExplanationPreference? = null
    private var templatesGroupPref: DialogListExplanationPreference? = null
    private var dbMaxHistoryItemsPref: InputNumberPreference? = null
    private var dbMaxHistorySizePref: InputNumberPreference? = null
    private var mEncryptionAlgorithmPref: DialogListExplanationPreference? = null
    private var mKeyDerivationPref: DialogListExplanationPreference? = null
    private var mRoundPref: InputKdfNumberPreference? = null
    private var mMemoryPref: InputKdfSizePreference? = null
    private var mParallelismPref: InputKdfNumberPreference? = null

    private val menuProvider: MenuProvider = object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.database, menu)
            if (mDatabaseReadOnly) {
                menu.findItem(R.id.menu_save_database)?.isVisible = false
                menu.findItem(R.id.menu_merge_database)?.isVisible = false
            }
            if (!mMergeDataAllowed) {
                menu.findItem(R.id.menu_merge_database)?.isVisible = false
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_save_database -> {
                    saveDatabase(!mDatabaseReadOnly)
                    true
                }
                R.id.menu_merge_database -> {
                    mergeDatabase(!mDatabaseReadOnly)
                    true
                }
                R.id.menu_reload_database -> {
                    reloadDatabase()
                    true
                }
                R.id.menu_app_settings -> {
                    // Check the time lock before launching settings
                    // TODO activity menu
                    (activity as SettingsActivity?)?.let {
                        SettingsActivity.launch(it, true)
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner)

        mDatabaseViewModel.database.observe(viewLifecycleOwner) { database ->
            mDatabase = database
            view.resetAppTimeoutWhenViewTouchedOrFocused(requireContext(), database?.loaded)
            onDatabaseRetrieved(database)
        }

        mDatabaseViewModel.actionFinished.observe(viewLifecycleOwner) {
            onDatabaseActionFinished(it.database, it.actionTask, it.result)
        }
    }

    override fun onCreateScreenPreference(screen: Screen, savedInstanceState: Bundle?, rootKey: String?) {
        mScreen = screen
        val database = mDatabase
        // Load the preferences from an XML resource
        when (screen) {
            Screen.DATABASE -> {
                setPreferencesFromResource(R.xml.preferences_database, rootKey)
                if (database?.loaded == true)
                    onCreateDatabasePreference(database)
            }
            Screen.DATABASE_SECURITY -> {
                setPreferencesFromResource(R.xml.preferences_database_security, rootKey)
                if (database?.loaded == true)
                    onCreateDatabaseSecurityPreference(database)
            }
            Screen.DATABASE_MASTER_KEY -> {
                setPreferencesFromResource(R.xml.preferences_database_master_key, rootKey)
                if (database?.loaded == true)
                    onCreateDatabaseMasterKeyPreference(database)
            }
            else -> {
            }
        }
    }

    private fun saveDatabase(save: Boolean) {
        mDatabaseViewModel.saveDatabase(save)
    }

    private fun mergeDatabase(save: Boolean) {
        mDatabaseViewModel.mergeDatabase(save)
    }

    private fun reloadDatabase() {
        mDatabaseViewModel.reloadDatabase(false)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        mDatabase = database
        mDatabaseReadOnly = database?.isReadOnly == true
        mMergeDataAllowed = database?.isMergeDataAllowed() == true

        mDatabase?.let {
            if (it.loaded) {
                when (mScreen) {
                    Screen.DATABASE -> {
                        onCreateDatabasePreference(it)
                    }
                    Screen.DATABASE_SECURITY -> {
                        onCreateDatabaseSecurityPreference(it)
                    }
                    Screen.DATABASE_MASTER_KEY -> {
                        onCreateDatabaseMasterKeyPreference(it)
                    }
                    else -> {
                    }
                }
            } else {
                Log.e(javaClass.name, "Database isn't ready")
            }
        }
    }

    private fun onCreateDatabasePreference(database: ContextualDatabase) {
        val dbGeneralPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_general_key))

        // Database name
        dbNamePref = findPreference(getString(R.string.database_name_key))
        dbNamePref?.let { namePreference ->
            if (database.allowName) {
                namePreference.summary = database.name
            } else {
                dbGeneralPrefCategory?.removePreference(namePreference)
            }
        }

        // Database description
        dbDescriptionPref = findPreference(getString(R.string.database_description_key))
        dbDescriptionPref?.let { descriptionPreference ->
            if (database.allowDescription) {
                dbDescriptionPref?.summary = database.description
            } else {
                dbGeneralPrefCategory?.removePreference(descriptionPreference)
            }
        }

        // Database default username
        dbDefaultUsernamePref = findPreference(getString(R.string.database_default_username_key))
        dbDefaultUsernamePref?.summary = database.defaultUsername

        // Database custom color
        dbCustomColorPref = findPreference(getString(R.string.database_custom_color_key))
        dbCustomColorPref?.apply {
            val customColor = database.customColor
            if (customColor != null) {
                color = customColor
                summary = ChromaUtil.getFormattedColorString(customColor, false)
            } else{
                color = DialogColorPreference.DISABLE_COLOR
                summary = ""
            }
        }

        // Version
        findPreference<Preference>(getString(R.string.database_version_key))
            ?.summary = database.version

        val dbCompressionPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_data_key))

        // Database compression
        dbDataCompressionPref = findPreference(getString(R.string.database_data_compression_key))
        if (database.allowDataCompression) {
            dbDataCompressionPref?.summary = (database.compressionAlgorithm
                ?: CompressionAlgorithm.NONE).getLocalizedName(resources)
        } else {
            dbCompressionPrefCategory?.isVisible = false
        }

        val dbRecycleBinPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_recycle_bin_key))
        recycleBinGroupPref = findPreference(getString(R.string.recycle_bin_group_key))

        // Recycle bin
        if (database.allowConfigurableRecycleBin) {
            val recycleBinEnablePref: TwoStatePreference? = findPreference(getString(R.string.recycle_bin_enable_key))
            recycleBinEnablePref?.apply {
                isChecked = database.isRecycleBinEnabled
                isEnabled = if (!mDatabaseReadOnly) {
                    setOnPreferenceChangeListener { _, newValue ->
                        val recycleBinEnabled = newValue as Boolean
                        database.enableRecycleBin(recycleBinEnabled, resources.getString(R.string.recycle_bin))
                        refreshRecycleBinGroup(database)
                        // Save the database if not in readonly mode
                        saveDatabase(mDatabaseAutoSaveEnabled)
                        true
                    }
                    true
                } else {
                    false
                }
            }
            // Change the recycle bin group
            recycleBinGroupPref?.setOnPreferenceClickListener {

                true
            }
            // Recycle Bin group
            refreshRecycleBinGroup(database)
        } else {
            recycleBinGroupPref?.onPreferenceClickListener = null
            dbRecycleBinPrefCategory?.isVisible = false
        }

        // Templates
        val templatesGroupPrefCategory: PreferenceCategory? = findPreference(getString(R.string.database_category_templates_key))
        templatesGroupPref = findPreference(getString(R.string.templates_group_uuid_key))
        if (database.allowTemplatesGroup) {
            val templatesEnablePref: TwoStatePreference? = findPreference(getString(R.string.templates_group_enable_key))
            templatesEnablePref?.apply {
                isChecked = database.isTemplatesEnabled
                isEnabled = if (!mDatabaseReadOnly) {
                    setOnPreferenceChangeListener { _, newValue ->
                        val templatesEnabled = newValue as Boolean
                        database.enableTemplates(templatesEnabled,
                            resources.getString(R.string.templates)
                        )
                        refreshTemplatesGroup(database)
                        // Save the database if not in readonly mode
                        saveDatabase(mDatabaseAutoSaveEnabled)
                        true
                    }
                    true
                } else {
                    false
                }
            }
            // Refresh templates group
            refreshTemplatesGroup(database)
        } else {
            templatesGroupPrefCategory?.isVisible = false
        }

        // History
        findPreference<PreferenceCategory>(getString(R.string.database_category_history_key))
            ?.isVisible = database.manageHistory == true

        // Max history items
        dbMaxHistoryItemsPref = findPreference<InputNumberPreference>(getString(R.string.max_history_items_key))?.apply {
            summary = database.historyMaxItems.toString()
        }

        // Max history size
        dbMaxHistorySizePref = findPreference<InputNumberPreference>(getString(R.string.max_history_size_key))?.apply {
            summary = database.historyMaxSize.toString()
        }
    }

    private fun refreshRecycleBinGroup(database: ContextualDatabase?) {
        recycleBinGroupPref?.apply {
            if (database?.isRecycleBinEnabled == true) {
                summary = database.recycleBin?.toString()
                isEnabled = true
            } else {
                summary = null
                isEnabled = false
            }
        }
    }

    private fun refreshTemplatesGroup(database: ContextualDatabase?) {
        templatesGroupPref?.apply {
            if (database?.isTemplatesEnabled == true) {
                summary = database.templatesGroup?.toString()
                isEnabled = true
            } else {
                summary = null
                isEnabled = false
            }
        }
    }

    private fun onCreateDatabaseSecurityPreference(database: ContextualDatabase) {
        // Encryption Algorithm
        mEncryptionAlgorithmPref = findPreference<DialogListExplanationPreference>(getString(R.string.encryption_algorithm_key))?.apply {
            summary = database.getEncryptionAlgorithmName()
        }

        // Key derivation function
        mKeyDerivationPref = findPreference<DialogListExplanationPreference>(getString(R.string.key_derivation_function_key))?.apply {
            summary = database.getKeyDerivationName()
        }

        // Round encryption
        mRoundPref = findPreference<InputKdfNumberPreference>(getString(R.string.transform_rounds_key))?.apply {
            summary = database.numberKeyEncryptionRounds.toString()
        }

        // Memory Usage
        mMemoryPref = findPreference<InputKdfSizePreference>(getString(R.string.memory_usage_key))?.apply {
            summary = database.memoryUsage.toString()
        }

        // Parallelism
        mParallelismPref = findPreference<InputKdfNumberPreference>(getString(R.string.parallelism_key))?.apply {
            summary = database.parallelism.toString()
        }
    }

    private fun onCreateDatabaseMasterKeyPreference(database: ContextualDatabase) {
        findPreference<Preference>(getString(R.string.settings_database_change_credentials_key))?.apply {
            isEnabled = if (!mDatabaseReadOnly) {
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    SetMainCredentialDialogFragment.getInstance(database.allowNoMasterKey)
                            .show(parentFragmentManager, "passwordDialog")
                    false
                }
                true
            } else {
                false
            }
        }
    }

    private val colorSelectedListener: ((Int?)-> Unit) = { color ->
        if (color != null) {
            dbCustomColorPref?.color = color
            dbCustomColorPref?.summary = ChromaUtil.getFormattedColorString(color, false)
        } else {
            dbCustomColorPref?.color = DialogColorPreference.DISABLE_COLOR
            dbCustomColorPref?.summary = ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        try {
            // To reassign color listener after orientation change
            val chromaDialog = parentFragmentManager.findFragmentByTag(TAG_PREF_FRAGMENT) as DatabaseColorPreferenceDialogFragmentCompat?
            chromaDialog?.onColorSelectedListener = colorSelectedListener
        } catch (e: Exception) {}

        return view
    }

    // TODO check error
    override fun onDatabaseActionFinished(database: ContextualDatabase,
                                          actionTask: String,
                                          result: ActionRunnable.Result) {
        result.data?.let { data ->
            if (data.containsKey(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                    && data.containsKey(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)) {
                when (actionTask) {
                    /*
                    --------
                    Main preferences
                    --------
                    */
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_NAME_TASK -> {
                        val oldName = data.getString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)!!
                        val newName = data.getString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)!!
                        val nameToShow =
                                if (result.isSuccess) {
                                    newName
                                } else {
                                    database.name = oldName
                                    oldName
                                }
                        dbNamePref?.summary = nameToShow
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_DESCRIPTION_TASK -> {
                        val oldDescription = data.getString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)!!
                        val newDescription = data.getString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)!!
                        val descriptionToShow =
                                if (result.isSuccess) {
                                    newDescription
                                } else {
                                    database.description = oldDescription
                                    oldDescription
                                }
                        dbDescriptionPref?.summary = descriptionToShow
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_DEFAULT_USERNAME_TASK -> {
                        val oldDefaultUsername = data.getString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)!!
                        val newDefaultUsername = data.getString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)!!
                        val defaultUsernameToShow =
                                if (result.isSuccess) {
                                    newDefaultUsername
                                } else {
                                    mDatabase?.defaultUsername = oldDefaultUsername
                                    oldDefaultUsername
                                }
                        dbDefaultUsernamePref?.summary = defaultUsernameToShow
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_COLOR_TASK -> {
                        val oldColor = data.getString(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)!!
                        val newColor = data.getString(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)!!

                        val defaultColorToShow =
                                if (result.isSuccess) {
                                    newColor
                                } else {
                                    mDatabase?.customColor = Color.parseColor(oldColor)
                                    oldColor
                                }
                        dbCustomColorPref?.summary = defaultColorToShow
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_COMPRESSION_TASK -> {
                        val oldCompression = data.getSerializableCompat<CompressionAlgorithm>(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newCompression = data.getSerializableCompat<CompressionAlgorithm>(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val algorithmToShow =
                                if (result.isSuccess) {
                                    newCompression
                                } else {
                                    mDatabase?.compressionAlgorithm = oldCompression
                                    oldCompression
                                }
                        dbDataCompressionPref?.summary = algorithmToShow?.getLocalizedName(resources)
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_RECYCLE_BIN_TASK -> {
                        val oldRecycleBin = data.getParcelableCompat<Group>(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newRecycleBin = data.getParcelableCompat<Group>(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val recycleBinToShow =
                                if (result.isSuccess) {
                                    newRecycleBin
                                } else {
                                    oldRecycleBin
                                }
                        mDatabase?.setRecycleBin(recycleBinToShow)
                        refreshRecycleBinGroup(database)
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_TEMPLATES_GROUP_TASK -> {
                        val oldTemplatesGroup = data.getParcelableCompat<Group>(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newTemplatesGroup = data.getParcelableCompat<Group>(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val templatesGroupToShow =
                            if (result.isSuccess) {
                                newTemplatesGroup
                            } else {
                                oldTemplatesGroup
                            }
                        mDatabase?.setTemplatesGroup(templatesGroupToShow)
                        refreshTemplatesGroup(database)
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_MAX_HISTORY_ITEMS_TASK -> {
                        val oldMaxHistoryItems = data.getInt(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newMaxHistoryItems = data.getInt(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val maxHistoryItemsToShow =
                                if (result.isSuccess) {
                                    newMaxHistoryItems
                                } else {
                                    mDatabase?.historyMaxItems = oldMaxHistoryItems
                                    oldMaxHistoryItems
                                }
                        dbMaxHistoryItemsPref?.summary = maxHistoryItemsToShow.toString()
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_MAX_HISTORY_SIZE_TASK -> {
                        val oldMaxHistorySize = data.getLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newMaxHistorySize = data.getLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val maxHistorySizeToShow =
                                if (result.isSuccess) {
                                    newMaxHistorySize
                                } else {
                                    mDatabase?.historyMaxSize = oldMaxHistorySize
                                    oldMaxHistorySize
                                }
                        dbMaxHistorySizePref?.summary = maxHistorySizeToShow.toString()
                    }

                    /*
                    --------
                    Security
                    --------
                     */
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_ENCRYPTION_TASK -> {
                        val oldEncryption = data.getSerializableCompat<EncryptionAlgorithm>(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newEncryption = data.getSerializableCompat<EncryptionAlgorithm>(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val algorithmToShow =
                                if (result.isSuccess) {
                                    newEncryption
                                } else {
                                    mDatabase?.encryptionAlgorithm = oldEncryption
                                    oldEncryption
                                }
                        mEncryptionAlgorithmPref?.summary = algorithmToShow.toString()
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_KEY_DERIVATION_TASK -> {
                        val oldKeyDerivationEngine = data.getSerializableCompat<KdfEngine>(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newKeyDerivationEngine = data.getSerializableCompat<KdfEngine>(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val kdfEngineToShow =
                                if (result.isSuccess) {
                                    newKeyDerivationEngine
                                } else {
                                    mDatabase?.kdfEngine = oldKeyDerivationEngine
                                    oldKeyDerivationEngine
                                }
                        mKeyDerivationPref?.summary = kdfEngineToShow.toString()

                        mRoundPref?.summary = kdfEngineToShow?.defaultKeyRounds.toString()
                        // Disable memory and parallelism if not available
                        mMemoryPref?.summary = kdfEngineToShow?.defaultMemoryUsage.toString()
                        mParallelismPref?.summary = kdfEngineToShow?.defaultParallelism.toString()
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_ITERATIONS_TASK -> {
                        val oldIterations = data.getLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newIterations = data.getLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val roundsToShow =
                                if (result.isSuccess) {
                                    newIterations
                                } else {
                                    mDatabase?.numberKeyEncryptionRounds = oldIterations
                                    oldIterations
                                }
                        mRoundPref?.summary = roundsToShow.toString()
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_MEMORY_USAGE_TASK -> {
                        val oldMemoryUsage = data.getLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newMemoryUsage = data.getLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val memoryToShow =
                                if (result.isSuccess) {
                                    newMemoryUsage
                                } else {
                                    mDatabase?.memoryUsage = oldMemoryUsage
                                    oldMemoryUsage
                                }
                        mMemoryPref?.summary = memoryToShow.toString()
                    }
                    DatabaseTaskNotificationService.ACTION_DATABASE_UPDATE_PARALLELISM_TASK -> {
                        val oldParallelism = data.getLong(DatabaseTaskNotificationService.OLD_ELEMENT_KEY)
                        val newParallelism = data.getLong(DatabaseTaskNotificationService.NEW_ELEMENT_KEY)
                        val parallelismToShow =
                                if (result.isSuccess) {
                                    newParallelism
                                } else {
                                    mDatabase?.parallelism = oldParallelism
                                    oldParallelism
                                }
                        mParallelismPref?.summary = parallelismToShow.toString()
                    }
                }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        var otherDialogFragment = false

        var dialogFragment: DialogFragment? = null
        // Main Preferences
        when (preference.key) {
            getString(R.string.database_name_key) -> {
                dialogFragment = DatabaseNamePreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.database_description_key) -> {
                dialogFragment = DatabaseDescriptionPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.database_default_username_key) -> {
                dialogFragment = DatabaseDefaultUsernamePreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.database_custom_color_key) -> {
                dialogFragment = DatabaseColorPreferenceDialogFragmentCompat.newInstance(preference.key).apply {
                    onColorSelectedListener = colorSelectedListener
                }
            }
            getString(R.string.database_data_compression_key) -> {
                dialogFragment = DatabaseDataCompressionPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.database_data_remove_unlinked_attachments_key) -> {
                dialogFragment = DatabaseRemoveUnlinkedDataPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.recycle_bin_group_key) -> {
                dialogFragment = DatabaseRecycleBinGroupPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.templates_group_uuid_key) -> {
                dialogFragment = DatabaseTemplatesGroupPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.max_history_items_key) -> {
                dialogFragment = DatabaseMaxHistoryItemsPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.max_history_size_key) -> {
                dialogFragment = DatabaseMaxHistorySizePreferenceDialogFragmentCompat.newInstance(preference.key)
            }

            // Security
            getString(R.string.encryption_algorithm_key) -> {
                dialogFragment = DatabaseEncryptionAlgorithmPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.key_derivation_function_key) -> {
                val keyDerivationDialogFragment = DatabaseKeyDerivationPreferenceDialogFragmentCompat.newInstance(preference.key)
                // Add other prefs to manage
                keyDerivationDialogFragment.setRoundPreference(mRoundPref)
                keyDerivationDialogFragment.setMemoryPreference(mMemoryPref)
                keyDerivationDialogFragment.setParallelismPreference(mParallelismPref)
                dialogFragment = keyDerivationDialogFragment
            }
            getString(R.string.transform_rounds_key) -> {
                dialogFragment = DatabaseRoundsPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.memory_usage_key) -> {
                dialogFragment = DatabaseMemoryUsagePreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            getString(R.string.parallelism_key) -> {
                dialogFragment = DatabaseParallelismPreferenceDialogFragmentCompat.newInstance(preference.key)
            }
            else -> otherDialogFragment = true
        }

        if (dialogFragment != null && !mDatabaseReadOnly) {
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

        context?.let { context ->
            mDatabaseAutoSaveEnabled = PreferencesUtil.isAutoSaveDatabaseEnabled(context)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // To reload group when database settings are modified
        when (preference.key) {
            getString(R.string.database_name_key),
            getString(R.string.database_description_key),
            getString(R.string.database_default_username_key),
            getString(R.string.database_custom_color_key),
            getString(R.string.database_data_compression_key),
            getString(R.string.database_data_remove_unlinked_attachments_key),
            getString(R.string.recycle_bin_enable_key),
            getString(R.string.recycle_bin_group_key),
            getString(R.string.templates_group_enable_key),
            getString(R.string.templates_group_uuid_key),
            getString(R.string.max_history_items_key),
            getString(R.string.max_history_size_key) -> {
                NestedAppSettingsFragment.DATABASE_PREFERENCE_CHANGED = true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    companion object {
        private const val TAG_PREF_FRAGMENT = "TAG_PREF_FRAGMENT"
    }
}
