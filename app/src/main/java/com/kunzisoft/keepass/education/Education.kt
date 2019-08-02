package com.kunzisoft.keepass.education

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R

open class Education(val activity: Activity) {

    /**
     * Utility method to save preference after an education action
     */
    protected fun checkAndPerformedEducation(isEducationAlreadyPerformed: Boolean,
                                   tapTarget: TapTarget,
                                   listener: TapTargetView.Listener,
                                   saveEducationStringId: Int): Boolean {
        var doEducation = false
        if (isEducationScreensEnabled()) {
            if (isEducationAlreadyPerformed) {
                try {
                    TapTargetView.showFor(activity, tapTarget, listener)
                    saveEducationPreference(activity, saveEducationStringId)
                    doEducation = true
                } catch (e: Exception) {
                    Log.w(Education::class.java.name, "Can't performed education " + e.message)
                }
            }
        }
        return doEducation
    }


    /**
     * Define if educations screens are enabled
     */
    fun isEducationScreensEnabled(): Boolean {
        return isEducationScreensEnabled(activity)
    }

    /**
     * Register education preferences as true in EDUCATION_PREFERENCE SharedPreferences
     *
     * @param context The context to retrieve the key string in XML
     * @param educationKeys Keys to save as boolean 'true'
     */
    fun saveEducationPreference(context: Context, vararg educationKeys: Int) {
        val sharedPreferences = getEducationSharedPreferences(context)
        val editor = sharedPreferences.edit()
        for (key in educationKeys) {
            editor.putBoolean(context.getString(key), true)
        }
        editor.apply()
    }

    companion object {

        private const val EDUCATION_PREFERENCE = "kdbxeducation"

        /**
         * All preference keys associated with education
         */
        val educationResourcesKeys = intArrayOf(
                R.string.education_create_db_key,
                R.string.education_select_db_key,
                R.string.education_open_link_db_key,
                R.string.education_unlock_key,
                R.string.education_read_only_key,
                R.string.education_fingerprint_key,
                R.string.education_search_key,
                R.string.education_new_node_key,
                R.string.education_sort_key,
                R.string.education_lock_key,
                R.string.education_copy_username_key,
                R.string.education_entry_edit_key,
                R.string.education_password_generator_key,
                R.string.education_entry_new_field_key)


        /**
         * Get preferences bundle for education
         */
        fun getEducationSharedPreferences(ctx: Context): SharedPreferences {
            return ctx.getSharedPreferences(
                    EDUCATION_PREFERENCE,
                    Context.MODE_PRIVATE)
        }

        /**
         * Define if educations screens are enabled
         */
        fun isEducationScreensEnabled(context: Context): Boolean {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getBoolean(context.getString(R.string.enable_education_screens_key),
                    context.resources.getBoolean(R.bool.enable_education_screens_default))
        }

        /**
         * Determines whether the explanatory view of the database creation has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_create_db_key key
         */
        fun isEducationCreateDatabasePerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_create_db_key),
                    context.resources.getBoolean(R.bool.education_create_db_default))
        }

        /**
         * Determines whether the explanatory view of the database selection has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_select_db_key key
         */
        fun isEducationSelectDatabasePerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_select_db_key),
                    context.resources.getBoolean(R.bool.education_select_db_default))
        }

        /**
         * Determines whether the explanatory view of the database selection has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_select_db_key key
         */
        fun isEducationOpenLinkDatabasePerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_open_link_db_key),
                    context.resources.getBoolean(R.bool.education_open_link_db_default))
        }

        /**
         * Determines whether the explanatory view of the database unlock has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_unlock_key key
         */
        fun isEducationUnlockPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_unlock_key),
                    context.resources.getBoolean(R.bool.education_unlock_default))
        }

        /**
         * Determines whether the explanatory view of the database read-only has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_read_only_key key
         */
        fun isEducationReadOnlyPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_read_only_key),
                    context.resources.getBoolean(R.bool.education_read_only_default))
        }

        /**
         * Determines whether the explanatory view of the fingerprint unlock has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_fingerprint_key key
         */
        fun isEducationFingerprintPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_fingerprint_key),
                    context.resources.getBoolean(R.bool.education_fingerprint_default))
        }

        /**
         * Determines whether the explanatory view of search has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_search_key key
         */
        fun isEducationSearchPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_search_key),
                    context.resources.getBoolean(R.bool.education_search_default))
        }

        /**
         * Determines whether the explanatory view of add new node has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_new_node_key key
         */
        fun isEducationNewNodePerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_new_node_key),
                    context.resources.getBoolean(R.bool.education_new_node_default))
        }

        /**
         * Determines whether the explanatory view of the sort has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_sort_key key
         */
        fun isEducationSortPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_sort_key),
                    context.resources.getBoolean(R.bool.education_sort_default))
        }

        /**
         * Determines whether the explanatory view of the database lock has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_lock_key key
         */
        fun isEducationLockPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_lock_key),
                    context.resources.getBoolean(R.bool.education_lock_default))
        }

        /**
         * Determines whether the explanatory view of the username copy has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_copy_username_key key
         */
        fun isEducationCopyUsernamePerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_copy_username_key),
                    context.resources.getBoolean(R.bool.education_copy_username_key))
        }

        /**
         * Determines whether the explanatory view of the entry edition has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_entry_edit_key key
         */
        fun isEducationEntryEditPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_entry_edit_key),
                    context.resources.getBoolean(R.bool.education_entry_edit_default))
        }

        /**
         * Determines whether the explanatory view of the password generator has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_password_generator_key key
         */
        fun isEducationPasswordGeneratorPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_password_generator_key),
                    context.resources.getBoolean(R.bool.education_password_generator_default))
        }

        /**
         * Determines whether the explanatory view of the new fields button in an entry has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_entry_new_field_key key
         */
        fun isEducationEntryNewFieldPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_entry_new_field_key),
                    context.resources.getBoolean(R.bool.education_entry_new_field_default))
        }

        /**
         * Defines if the reset education preference has been reclicked
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_screen_reclicked_key key
         */
        fun isEducationScreenReclickedPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_screen_reclicked_key),
                    context.resources.getBoolean(R.bool.education_screen_reclicked_default))
        }
    }
}