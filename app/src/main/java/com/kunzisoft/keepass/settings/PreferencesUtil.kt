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

import android.app.backup.BackupManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.Stylish
import com.kunzisoft.keepass.biometric.AdvancedUnlockManager
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.education.Education
import com.kunzisoft.keepass.timeout.TimeoutHelper
import java.util.*

object PreferencesUtil {

    fun saveDefaultDatabasePath(context: Context, defaultDatabaseUri: Uri?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs?.edit()?.apply {
            defaultDatabaseUri?.let {
                putString(context.getString(R.string.default_database_path_key), it.toString())
            } ?: kotlin.run {
                remove(context.getString(R.string.default_database_path_key))
            }
            apply()
        }
        BackupManager(context).dataChanged()
    }

    fun getDefaultDatabasePath(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(context.getString(R.string.default_database_path_key), "")
    }

    fun saveNodeSort(context: Context,
                     sortNodeEnum: SortNodeEnum,
                     sortNodeParameters: SortNodeEnum.SortNodeParameters) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs?.edit()?.apply {
            putString(context.getString(R.string.sort_node_key), sortNodeEnum.name)
            putBoolean(context.getString(R.string.sort_ascending_key), sortNodeParameters.ascending)
            putBoolean(context.getString(R.string.sort_group_before_key), sortNodeParameters.groupsBefore)
            putBoolean(context.getString(R.string.sort_recycle_bin_bottom_key), sortNodeParameters.recycleBinBottom)
            apply()
        }
    }

    fun rememberDatabaseLocations(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.remember_database_locations_key),
                context.resources.getBoolean(R.bool.remember_database_locations_default))
    }

    fun showRecentFiles(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.show_recent_files_key),
                context.resources.getBoolean(R.bool.show_recent_files_default))
    }

    fun hideBrokenLocations(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.hide_broken_locations_key),
                context.resources.getBoolean(R.bool.hide_broken_locations_default))
    }

    fun rememberKeyFileLocations(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.remember_keyfile_locations_key),
                context.resources.getBoolean(R.bool.remember_keyfile_locations_default))
    }

    fun omitBackup(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.omit_backup_search_key),
                context.resources.getBoolean(R.bool.omit_backup_search_default))
    }

    fun automaticallyFocusSearch(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.auto_focus_search_key),
                context.resources.getBoolean(R.bool.auto_focus_search_default))
    }

    fun searchSubdomains(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.subdomain_search_key),
                context.resources.getBoolean(R.bool.subdomain_search_default))
    }

    fun showUsernamesListEntries(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.list_entries_show_username_key),
                context.resources.getBoolean(R.bool.list_entries_show_username_default))
    }

    fun showNumberEntries(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.list_groups_show_number_entries_key),
                context.resources.getBoolean(R.bool.list_groups_show_number_entries_default))
    }

    fun showExpiredEntries(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return ! prefs.getBoolean(context.getString(R.string.hide_expired_entries_key),
                context.resources.getBoolean(R.bool.hide_expired_entries_default))
    }

    fun showOTPToken(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.show_otp_token_key),
                context.resources.getBoolean(R.bool.show_otp_token_default))
    }

    fun showUUID(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.show_uuid_key),
                context.resources.getBoolean(R.bool.show_uuid_default))
    }

    fun getStyle(context: Context): String {
        val defaultStyleString = Stylish.defaultStyle(context)
        val styleString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.setting_style_key), defaultStyleString)
                ?: defaultStyleString
        // Return the system style
        return Stylish.retrieveEquivalentSystemStyle(context, styleString)
    }

    fun setStyle(context: Context, styleString: String) {
        var tempThemeString = styleString
        if (BuildConfig.CLOSED_STORE || !Education.isEducationScreenReclickedPerformed(context)) {
            if (tempThemeString in BuildConfig.STYLES_DISABLED) {
                tempThemeString = Stylish.defaultStyle(context)
            }
        }
        // Store light style to show selection in array list
        tempThemeString = Stylish.retrieveEquivalentLightStyle(context, tempThemeString)
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(R.string.setting_style_key), tempThemeString)
                .apply()
        Stylish.load(context)
    }

    fun getStyleBrightness(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(context.getString(R.string.setting_style_brightness_key),
                context.getString(R.string.list_style_brightness_follow_system))
    }

    /**
     * Retrieve the text size in % (1 for 100%)
     */
    fun getListTextSize(context: Context): Float {
        val index = try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val listSizeString = prefs.getString(context.getString(R.string.list_size_key),
                    context.getString(R.string.list_size_string_medium))
            context.resources.getStringArray(R.array.list_size_string_values).indexOf(listSizeString)
        } catch (e: Exception) {
            1
        }
        val typedArray = context.resources.obtainTypedArray(R.array.list_size_values)
        val listSize = typedArray.getFloat(index, 1.0F)
        typedArray.recycle()
        return listSize
    }

    fun getDefaultPasswordLength(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(context.getString(R.string.password_length_key),
                Integer.parseInt(context.getString(R.string.default_password_length)))
    }

    fun getDefaultPasswordCharacters(context: Context): Set<String>? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getStringSet(context.getString(R.string.list_password_generator_options_key),
                HashSet(listOf(*context.resources
                                .getStringArray(R.array.list_password_generator_options_default_values))))
    }

    fun isClipboardNotificationsEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.clipboard_notifications_key),
                context.resources.getBoolean(R.bool.clipboard_notifications_default))
    }

    /**
     * Save current time, can be retrieve with `getTimeSaved()`
     */
    fun saveCurrentTime(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
            putLong(context.getString(R.string.timeout_backup_key), System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Time previously saved in milliseconds (commonly used to compare with current time and check timeout)
     */
    fun getTimeSaved(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getLong(context.getString(R.string.timeout_backup_key),
                TimeoutHelper.NEVER)
    }

    /**
     * App timeout selected in milliseconds
     */
    fun getAppTimeout(context: Context): Long {
        return try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            (prefs.getString(context.getString(R.string.app_timeout_key),
                    context.getString(R.string.timeout_default)) ?: "300000").toLong()
        } catch (e: NumberFormatException) {
            TimeoutHelper.DEFAULT_TIMEOUT
        }
    }

    fun getClipboardTimeout(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(context.getString(R.string.clipboard_timeout_key),
                context.getString(R.string.clipboard_timeout_default))?.toLong()
                ?: TimeoutHelper.DEFAULT_TIMEOUT
    }

    fun getAdvancedUnlockTimeout(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(context.getString(R.string.temp_advanced_unlock_timeout_key),
                context.getString(R.string.temp_advanced_unlock_timeout_default))?.toLong()
                ?: TimeoutHelper.DEFAULT_TIMEOUT
    }

    fun isLockDatabaseWhenScreenShutOffEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.lock_database_screen_off_key),
                context.resources.getBoolean(R.bool.lock_database_screen_off_default))
    }

    fun isLockDatabaseWhenBackButtonOnRootClicked(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.lock_database_back_root_key),
                context.resources.getBoolean(R.bool.lock_database_back_root_default))
    }

    fun showLockDatabaseButton(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.lock_database_show_button_key),
                context.resources.getBoolean(R.bool.lock_database_show_button_default))
    }

    fun isAutoSaveDatabaseEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.enable_auto_save_database_key),
                context.resources.getBoolean(R.bool.enable_auto_save_database_default))
    }

    fun isAdvancedUnlockEnable(context: Context): Boolean {
        return isBiometricUnlockEnable(context) || isDeviceCredentialUnlockEnable(context)
    }

    fun isBiometricUnlockEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val biometricSupported = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            AdvancedUnlockManager.biometricUnlockSupported(context)
        } else {
            false
        }
        return prefs.getBoolean(context.getString(R.string.biometric_unlock_enable_key),
                context.resources.getBoolean(R.bool.biometric_unlock_enable_default))
                && biometricSupported
    }

    fun isDeviceCredentialUnlockEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Priority to biometric unlock
        val biometricAlreadySupported = isBiometricUnlockEnable(context)
        return prefs.getBoolean(context.getString(R.string.device_credential_unlock_enable_key),
                context.resources.getBoolean(R.bool.device_credential_unlock_enable_default))
                && !biometricAlreadySupported
    }

    fun isTempAdvancedUnlockEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.temp_advanced_unlock_enable_key),
                context.resources.getBoolean(R.bool.temp_advanced_unlock_enable_default))
    }

    fun isAdvancedUnlockPromptAutoOpenEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.biometric_auto_open_prompt_key),
                context.resources.getBoolean(R.bool.biometric_auto_open_prompt_default))
    }

    fun getListSort(context: Context): SortNodeEnum {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.getString(context.getString(R.string.sort_node_key),
                    SortNodeEnum.DB.name)?.let {
                return SortNodeEnum.valueOf(it)
            }
        } catch (e: Exception) {}
        return SortNodeEnum.DB
    }

    fun getGroupsBeforeSort(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.sort_group_before_key),
                context.resources.getBoolean(R.bool.sort_group_before_default))
    }

    fun getAscendingSort(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.sort_ascending_key),
                context.resources.getBoolean(R.bool.sort_ascending_default))
    }

    fun getRecycleBinBottomSort(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.sort_recycle_bin_bottom_key),
                context.resources.getBoolean(R.bool.sort_recycle_bin_bottom_default))
    }

    fun hideProtectedValue(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.hide_password_key),
                context.resources.getBoolean(R.bool.hide_password_default))
    }

    fun fieldFontIsInVisibility(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.monospace_font_fields_enable_key),
                context.resources.getBoolean(R.bool.monospace_font_fields_enable_default))
    }

    fun isFirstTimeAskAllowCopyProtectedFields(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.allow_copy_password_first_time_key),
                context.resources.getBoolean(R.bool.allow_copy_password_first_time_default))
    }

    fun allowCopyProtectedFields(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.allow_copy_password_key),
                context.resources.getBoolean(R.bool.allow_copy_password_default))
    }

    fun isClearClipboardNotificationEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.clear_clipboard_notification_key),
                context.resources.getBoolean(R.bool.clear_clipboard_notification_default))
    }

    fun isClearKeyboardNotificationEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_notification_entry_clear_close_key),
                context.resources.getBoolean(R.bool.keyboard_notification_entry_clear_close_default))
    }

    fun setAllowCopyPasswordAndProtectedFields(context: Context, allowCopy: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
                .putBoolean(context.getString(R.string.allow_copy_password_first_time_key), false)
                .putBoolean(context.getString(R.string.allow_copy_password_key), allowCopy)
                .apply()
    }

    fun getIconPackSelectedId(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(
                context.getString(R.string.setting_icon_pack_choose_key),
                context.getString(R.string.setting_icon_pack_choose_default))
    }

    fun emptyPasswordAllowed(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.allow_no_password_key),
                context.resources.getBoolean(R.bool.allow_no_password_default))
    }

    fun enableReadOnlyDatabase(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.enable_read_only_key),
                context.resources.getBoolean(R.bool.enable_read_only_default))
    }

    fun deletePasswordAfterConnexionAttempt(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.delete_entered_password_key),
                context.resources.getBoolean(R.bool.delete_entered_password_default))
    }

    fun isKeyboardNotificationEntryEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_notification_entry_key),
                context.resources.getBoolean(R.bool.keyboard_notification_entry_default))
    }

    fun isKeyboardEntrySelectionEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_selection_entry_key),
                context.resources.getBoolean(R.bool.keyboard_selection_entry_default))
    }

    fun isKeyboardSearchShareEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_search_share_key),
                context.resources.getBoolean(R.bool.keyboard_search_share_default))
    }

    fun isKeyboardSaveSearchInfoEnable(context: Context): Boolean {
        if (!isKeyboardSearchShareEnable(context))
            return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_save_search_info_key),
                context.resources.getBoolean(R.bool.keyboard_save_search_info_default))
    }

    fun isAutoGoActionEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_auto_go_action_key),
                context.resources.getBoolean(R.bool.keyboard_auto_go_action_default))
    }

    fun isKeyboardVibrationEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_key_vibrate_key),
                context.resources.getBoolean(R.bool.keyboard_key_vibrate_default))
    }

    fun isKeyboardSoundEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_key_sound_key),
                context.resources.getBoolean(R.bool.keyboard_key_sound_default))
    }

    fun isKeyboardPreviousDatabaseCredentialsEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_previous_database_credentials_key),
                context.resources.getBoolean(R.bool.keyboard_previous_database_credentials_default))
    }

    fun isKeyboardPreviousFillInEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_previous_fill_in_key),
                context.resources.getBoolean(R.bool.keyboard_previous_fill_in_default))
    }

    fun isKeyboardPreviousLockEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.keyboard_previous_lock_key),
                context.resources.getBoolean(R.bool.keyboard_previous_lock_default))
    }

    fun isAutofillCloseDatabaseEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_close_database_key),
                context.resources.getBoolean(R.bool.autofill_close_database_default))
    }

    fun isAutofillAutoSearchEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_auto_search_key),
                context.resources.getBoolean(R.bool.autofill_auto_search_default))
    }

    fun isAutofillInlineSuggestionsEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_inline_suggestions_key),
                context.resources.getBoolean(R.bool.autofill_inline_suggestions_default))
    }

    fun isAutofillManualSelectionEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_manual_selection_key),
                context.resources.getBoolean(R.bool.autofill_manual_selection_default))
    }

    fun isAutofillOTPSelectionEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_otp_selection_key),
            context.resources.getBoolean(R.bool.autofill_otp_selection_default))
    }

    fun isAutofillSaveSearchInfoEnable(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_save_search_info_key),
                context.resources.getBoolean(R.bool.autofill_save_search_info_default))
    }

    fun askToSaveAutofillData(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(context.getString(R.string.autofill_ask_to_save_data_key),
                context.resources.getBoolean(R.bool.autofill_ask_to_save_data_default))
    }

    /**
     * Retrieve the default Blocklist for application ID, including the current app
     */
    fun getDefaultApplicationIdBlocklist(resources: Resources?): Set<String> {
        return resources?.getStringArray(R.array.autofill_application_id_blocklist_default)
                ?.toMutableSet()?.apply {
                    add(BuildConfig.APPLICATION_ID)
                } ?: emptySet()
    }

    fun applicationIdBlocklist(context: Context): Set<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getStringSet(context.getString(R.string.autofill_application_id_blocklist_key),
                getDefaultApplicationIdBlocklist(context.resources))
                ?: emptySet()
    }

    fun webDomainBlocklist(context: Context): Set<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getStringSet(context.getString(R.string.autofill_web_domain_blocklist_key),
                context.resources.getStringArray(R.array.autofill_web_domain_blocklist_default).toMutableSet())
                ?: emptySet()
    }

    fun addApplicationIdToBlocklist(context: Context, applicationId: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val setItems: MutableSet<String> = applicationIdBlocklist(context).toMutableSet()
        setItems.add(applicationId)
        prefs.edit()
                .putStringSet(context.getString(R.string.autofill_application_id_blocklist_key), setItems)
                .apply()
    }

    fun addWebDomainToBlocklist(context: Context, webDomain: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val setItems: MutableSet<String> = webDomainBlocklist(context).toMutableSet()
        setItems.add(webDomain)
        prefs.edit()
                .putStringSet(context.getString(R.string.autofill_web_domain_blocklist_key), setItems)
                .apply()
    }

    fun getAppProperties(context: Context): Properties {
        val properties = Properties()
        for ((name, value) in PreferenceManager.getDefaultSharedPreferences(context).all) {
            properties[name] = value.toString()
        }
        for ((name, value) in Education.getEducationSharedPreferences(context).all) {
            properties[name] = value.toString()
        }
        return properties
    }

    private fun getStringSetFromProperties(value: String): Set<String> {
        return value.removePrefix("[")
                .removeSuffix("]")
                .split(", ")
                .toSet()
    }

    private fun putPropertiesInPreferences(properties: Properties,
                                           preferences: SharedPreferences,
                                           putProperty: (editor: SharedPreferences.Editor,
                                                         name: String,
                                                         value: String) -> Unit) {
        preferences.edit().apply {
            for ((name, value) in properties) {
                try {
                    putProperty(this, name as String, value as String)
                } catch (e:Exception) {
                    Log.e("PreferencesUtil", "Error when trying to parse app property $name=$value", e)
                }
            }
        }.apply()
    }

    fun setAppProperties(context: Context, properties: Properties) {
        putPropertiesInPreferences(properties,
                PreferenceManager.getDefaultSharedPreferences(context)) { editor, name, value ->
            when (name) {
                context.getString(R.string.allow_no_password_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.delete_entered_password_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.enable_read_only_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.enable_auto_save_database_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.omit_backup_search_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.auto_focus_search_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.subdomain_search_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.app_timeout_key) -> editor.putString(name, value.toLong().toString())
                context.getString(R.string.lock_database_screen_off_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.lock_database_back_root_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.lock_database_show_button_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.password_length_key) -> editor.putInt(name, value.toInt())
                context.getString(R.string.list_password_generator_options_key) -> editor.putStringSet(name, getStringSetFromProperties(value))
                context.getString(R.string.hide_password_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.allow_copy_password_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.remember_database_locations_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.show_recent_files_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.hide_broken_locations_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.remember_keyfile_locations_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.biometric_unlock_enable_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.device_credential_unlock_enable_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.biometric_auto_open_prompt_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.temp_advanced_unlock_enable_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.temp_advanced_unlock_timeout_key) -> editor.putString(name, value.toLong().toString())

                context.getString(R.string.magic_keyboard_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.clipboard_notifications_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.clear_clipboard_notification_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.clipboard_timeout_key) -> editor.putString(name, value.toLong().toString())
                context.getString(R.string.settings_autofill_enable_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_notification_entry_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_notification_entry_clear_close_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_entry_timeout_key) -> editor.putString(name, value.toLong().toString())
                context.getString(R.string.keyboard_selection_entry_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_search_share_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_save_search_info_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_auto_go_action_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_key_vibrate_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_key_sound_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_previous_database_credentials_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_previous_fill_in_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.keyboard_previous_lock_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_close_database_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_auto_search_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_inline_suggestions_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_manual_selection_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_otp_selection_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_save_search_info_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_ask_to_save_data_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.autofill_application_id_blocklist_key) -> editor.putStringSet(name, getStringSetFromProperties(value))
                context.getString(R.string.autofill_web_domain_blocklist_key) -> editor.putStringSet(name, getStringSetFromProperties(value))

                context.getString(R.string.setting_style_key) -> setStyle(context, value)
                context.getString(R.string.setting_style_brightness_key) -> editor.putString(name, value)
                context.getString(R.string.setting_icon_pack_choose_key) -> editor.putString(name, value)
                context.getString(R.string.list_entries_show_username_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.list_groups_show_number_entries_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.list_size_key) -> editor.putString(name, value)
                context.getString(R.string.monospace_font_fields_enable_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.hide_expired_entries_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.show_otp_token_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.show_uuid_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.enable_education_screens_key) -> editor.putBoolean(name, value.toBoolean())

                context.getString(R.string.sort_node_key) -> editor.putString(name, value)
                context.getString(R.string.sort_group_before_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.sort_ascending_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.sort_recycle_bin_bottom_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.allow_copy_password_first_time_key) -> editor.putBoolean(name, value.toBoolean())
            }
        }

        putPropertiesInPreferences(properties,
                Education.getEducationSharedPreferences(context)) { editor, name, value ->
            when (name) {
                context.getString(R.string.education_create_db_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_select_db_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_unlock_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_read_only_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_biometric_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_search_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_new_node_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_sort_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_lock_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_copy_username_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_entry_edit_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_password_generator_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_entry_new_field_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_add_attachment_key) -> editor.putBoolean(name, value.toBoolean())
                context.getString(R.string.education_setup_OTP_key) -> editor.putBoolean(name, value.toBoolean())
            }
        }
    }
}
