package com.kunzisoft.keepass.settings

import android.content.Context
import android.preference.PreferenceManager
import com.kunzisoft.keepass.database.R
import com.kunzisoft.keepass.timeout.TimeoutHelper

object DatabasePreferencesUtil {

    const val HIDE_EXPIRED_ENTRIES_KEY = "hide_expired_entries_key"
    private const val HIDE_EXPIRED_ENTRIES_DEFAULT = false
    const val SETTING_ICON_PACK_CHOOSE_KEY = "setting_icon_pack_choose_key"
    private const val SETTING_ICON_PACK_CHOOSE_DEFAULT = "material"
    const val SUBDOMAIN_SEARCH_KEY = "subdomain_search_key"
    private const val SUBDOMAIN_SEARCH_DEFAULT = false
    const val TIMEOUT_BACKUP_KEY = "timeout_backup_key"
    const val APP_TIMEOUT_KEY = "app_timeout_key"
    const val TIMEOUT_DEFAULT = "300000"

    fun showExpiredEntries(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return ! prefs.getBoolean(HIDE_EXPIRED_ENTRIES_KEY, HIDE_EXPIRED_ENTRIES_DEFAULT)
    }

    fun getIconPackSelectedId(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(
            SETTING_ICON_PACK_CHOOSE_KEY,
            SETTING_ICON_PACK_CHOOSE_DEFAULT)
    }

    fun searchSubdomains(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(SUBDOMAIN_SEARCH_KEY, SUBDOMAIN_SEARCH_DEFAULT)
    }

    /**
     * Save current time, can be retrieve with `getTimeSaved()`
     */
    fun saveCurrentTime(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
            putLong(TIMEOUT_BACKUP_KEY, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Time previously saved in milliseconds (commonly used to compare with current time and check timeout)
     */
    fun getTimeSaved(context: Context): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getLong(TIMEOUT_BACKUP_KEY,
            TimeoutHelper.NEVER)
    }

    /**
     * App timeout selected in milliseconds
     */
    fun getAppTimeout(context: Context): Long {
        return try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            (prefs.getString(APP_TIMEOUT_KEY, TIMEOUT_DEFAULT) ?: TIMEOUT_DEFAULT).toLong()
        } catch (e: NumberFormatException) {
            TimeoutHelper.DEFAULT_TIMEOUT
        }
    }
}
