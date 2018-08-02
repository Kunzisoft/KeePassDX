/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.SortNodeEnum;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PreferencesUtil {

    private static final String NO_BACKUP_PREFERENCE_FILE_NAME = "nobackup";
    private static final String EDUCATION_PREFERENCE = "kdbxeducation";

    public static SharedPreferences getNoBackupSharedPreferences(Context ctx) {
        return ctx.getSharedPreferences(
                PreferencesUtil.NO_BACKUP_PREFERENCE_FILE_NAME,
                Context.MODE_PRIVATE);
    }

    public static SharedPreferences getEducationSharedPreferences(Context ctx) {
        return ctx.getSharedPreferences(
                PreferencesUtil.EDUCATION_PREFERENCE,
                Context.MODE_PRIVATE);
    }

    public static void deleteAllValuesFromNoBackupPreferences(Context ctx) {
        SharedPreferences prefsNoBackup = getNoBackupSharedPreferences(ctx);
        SharedPreferences.Editor sharedPreferencesEditor = prefsNoBackup.edit();
        sharedPreferencesEditor.clear();
        sharedPreferencesEditor.apply();
    }

    public static boolean showUsernamesListEntries(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.list_entries_show_username_key),
                context.getResources().getBoolean(R.bool.list_entries_show_username_default));
    }

    /**
     * Retrieve the text size in SP, verify the integrity of the size stored in preference
     */
	public static float getListTextSize(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String defaultSizeString = ctx.getString(R.string.list_size_default);
        String listSize = prefs.getString(ctx.getString(R.string.list_size_key), defaultSizeString);
        if (!Arrays.asList(ctx.getResources().getStringArray(R.array.list_size_values)).contains(listSize))
            listSize = defaultSizeString;
        return Float.parseFloat(listSize);
	}

    public static int getDefaultPasswordLength(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getInt(ctx.getString(R.string.password_length_key),
                        Integer.parseInt(ctx.getString(R.string.default_password_length)));
    }

    public static Set<String> getDefaultPasswordCharacters(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getStringSet(ctx.getString(R.string.list_password_generator_options_key),
                new HashSet<>(Arrays.asList(
                        ctx.getResources()
                                .getStringArray(R.array.list_password_generator_options_default_values))));
    }

    public static boolean isClipboardNotificationsEnable(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.clipboard_notifications_key),
                ctx.getResources().getBoolean(R.bool.clipboard_notifications_default));
    }

    public static boolean isLockDatabaseWhenScreenShutOffEnable(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.lock_database_screen_off_key),
                ctx.getResources().getBoolean(R.bool.lock_database_screen_off_default));
    }

    public static boolean isFingerprintEnable(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.fingerprint_enable_key),
                ctx.getResources().getBoolean(R.bool.fingerprint_enable_default));
    }

    public static boolean isFullFilePathEnable(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.full_file_path_enable_key),
                ctx.getResources().getBoolean(R.bool.full_file_path_enable_default));
    }

    public static SortNodeEnum getListSort(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return SortNodeEnum.valueOf(prefs.getString(ctx.getString(R.string.sort_node_key),
                SortNodeEnum.TITLE.name()));
    }

    public static boolean getGroupsBeforeSort(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.sort_group_before_key),
                ctx.getResources().getBoolean(R.bool.sort_group_before_default));
    }

    public static boolean getAscendingSort(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.sort_ascending_key),
                ctx.getResources().getBoolean(R.bool.sort_ascending_default));
    }

    public static boolean getRecycleBinBottomSort(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.sort_recycle_bin_bottom_key),
                ctx.getResources().getBoolean(R.bool.sort_recycle_bin_bottom_default));
    }

    public static boolean isPasswordMask(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.maskpass_key),
                ctx.getResources().getBoolean(R.bool.maskpass_default));
    }

    public static boolean fieldFontIsInVisibility(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.monospace_font_fields_enable_key),
                ctx.getResources().getBoolean(R.bool.monospace_font_fields_enable_default));
    }

    public static boolean autoOpenSelectedFile(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.auto_open_file_uri_key),
                ctx.getResources().getBoolean(R.bool.auto_open_file_uri_default));
    }

    public static boolean isFirstTimeAskAllowCopyPasswordAndProtectedFields(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.allow_copy_password_first_time_key),
                ctx.getResources().getBoolean(R.bool.allow_copy_password_first_time_default));
    }

    public static boolean allowCopyPasswordAndProtectedFields(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(ctx.getString(R.string.allow_copy_password_key),
                ctx.getResources().getBoolean(R.bool.allow_copy_password_default));
    }

    public static void setAllowCopyPasswordAndProtectedFields(Context ctx, boolean allowCopy) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit()
                .putBoolean(ctx.getString(R.string.allow_copy_password_first_time_key), false)
                .putBoolean(ctx.getString(R.string.allow_copy_password_key), allowCopy)
                .apply();
    }

    public static String getIconPackSelectedId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(
                context.getString(R.string.setting_icon_pack_choose_key),
                context.getString(R.string.setting_icon_pack_choose_default));
    }

    public static boolean emptyPasswordAllowed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.allow_no_password_key),
                context.getResources().getBoolean(R.bool.allow_no_password_default));
    }

    public static boolean enableReadOnlyDatabase(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.enable_read_only_key),
                context.getResources().getBoolean(R.bool.enable_read_only_default));
    }

    /**
     * All preference keys associated with education
     */
    public static int[] educationResourceKeys = new int[] {
            R.string.education_create_db_key,
            R.string.education_select_db_key,
            R.string.education_open_link_db_key,
            R.string.education_unlock_key,
            R.string.education_read_only_key,
            R.string.education_search_key,
            R.string.education_new_node_key,
            R.string.education_sort_key,
            R.string.education_lock_key,
            R.string.education_copy_username_key,
            R.string.education_entry_edit_key,
            R.string.education_password_generator_key,
            R.string.education_entry_new_field_key
    };

    public static boolean isEducationScreensEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(context.getString(R.string.enable_education_screens_key),
                context.getResources().getBoolean(R.bool.enable_education_screens_default));
    }

    /**
     * Register education preferences as true in EDUCATION_PREFERENCE SharedPreferences
     *
     * @param context The context to retrieve the key string in XML
     * @param educationKeys Keys to save as boolean 'true'
     */
    public static void saveEducationPreference(Context context, int... educationKeys) {
        SharedPreferences sharedPreferences = PreferencesUtil.getEducationSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int key : educationKeys) {
            editor.putBoolean(context.getString(key), true);
        }
        editor.apply();
    }

    /**
     * Determines whether the explanatory view of the database creation has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_create_db_key key
     */
    public static boolean isEducationCreateDatabasePerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_create_db_key),
                context.getResources().getBoolean(R.bool.education_create_db_default));
    }

    /**
     * Determines whether the explanatory view of the database selection has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_select_db_key key
     */
    public static boolean isEducationSelectDatabasePerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_select_db_key),
                context.getResources().getBoolean(R.bool.education_select_db_default));
    }

    /**
     * Determines whether the explanatory view of the database selection has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_select_db_key key
     */
    public static boolean isEducationOpenLinkDatabasePerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_open_link_db_key),
                context.getResources().getBoolean(R.bool.education_open_link_db_default));
    }

    /**
     * Determines whether the explanatory view of the database unlock has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_unlock_key key
     */
    public static boolean isEducationUnlockPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_unlock_key),
                context.getResources().getBoolean(R.bool.education_unlock_default));
    }

    /**
     * Determines whether the explanatory view of the database read-only has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_read_only_key key
     */
    public static boolean isEducationReadOnlyPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_read_only_key),
                context.getResources().getBoolean(R.bool.education_read_only_default));
    }

    /**
     * Determines whether the explanatory view of search has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_search_key key
     */
    public static boolean isEducationSearchPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_search_key),
                context.getResources().getBoolean(R.bool.education_search_default));
    }

    /**
     * Determines whether the explanatory view of add new node has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_new_node_key key
     */
    public static boolean isEducationNewNodePerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_new_node_key),
                context.getResources().getBoolean(R.bool.education_new_node_default));
    }

    /**
     * Determines whether the explanatory view of the sort has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_sort_key key
     */
    public static boolean isEducationSortPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_sort_key),
                context.getResources().getBoolean(R.bool.education_sort_default));
    }

    /**
     * Determines whether the explanatory view of the database lock has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_lock_key key
     */
    public static boolean isEducationLockPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_lock_key),
                context.getResources().getBoolean(R.bool.education_lock_default));
    }

    /**
     * Determines whether the explanatory view of the username copy has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_copy_username_key key
     */
    public static boolean isEducationCopyUsernamePerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_copy_username_key),
                context.getResources().getBoolean(R.bool.education_copy_username_key));
    }

    /**
     * Determines whether the explanatory view of the entry edition has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_entry_edit_key key
     */
    public static boolean isEducationEntryEditPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_entry_edit_key),
                context.getResources().getBoolean(R.bool.education_entry_edit_default));
    }

    /**
     * Determines whether the explanatory view of the password generator has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_password_generator_key key
     */
    public static boolean isEducationPasswordGeneratorPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_password_generator_key),
                context.getResources().getBoolean(R.bool.education_password_generator_default));
    }

    /**
     * Determines whether the explanatory view of the new fields button in an entry has already been displayed.
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_entry_new_field_key key
     */
    public static boolean isEducationEntryNewFieldPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_entry_new_field_key),
                context.getResources().getBoolean(R.bool.education_entry_new_field_default));
    }

    /**
     * Defines if the reset education preference has been reclicked
     *
     * @param context The context to open the SharedPreferences
     * @return boolean value of education_screen_reclicked_key key
     */
    public static boolean isEducationScreenReclickedPerformed(Context context) {
        SharedPreferences prefs = getEducationSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.education_screen_reclicked_key),
                context.getResources().getBoolean(R.bool.education_screen_reclicked_default));
    }
}
