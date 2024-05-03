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
package com.kunzisoft.keepass.education

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.util.Log
import androidx.preference.PreferenceManager
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R

open class Education(val activity: Activity) {

    private var mOneEducationHintOpen = false
    /**
     * Utility method to save preference after an education action
     */
    protected fun checkAndPerformedEducation(isEducationAlreadyPerformed: Boolean,
                                             tapTarget: TapTarget,
                                             listener: TapTargetView.Listener,
                                             saveEducationStringId: Int): Boolean {
        var doEducation = false
        if (isEducationScreensEnabled()
            && !mOneEducationHintOpen
            && !isEducationAlreadyPerformed) {
            try {
                TapTargetView.showFor(activity, tapTarget, object : TapTargetView.Listener() {
                    override fun onTargetClick(view: TapTargetView) {
                        mOneEducationHintOpen = false
                        saveEducationPreference(activity, saveEducationStringId)
                        super.onTargetClick(view)
                        listener.onTargetClick(view)
                    }

                    override fun onOuterCircleClick(view: TapTargetView?) {
                        mOneEducationHintOpen = false
                        saveEducationPreference(activity, saveEducationStringId)
                        super.onOuterCircleClick(view)
                        listener.onOuterCircleClick(view)
                        view?.dismiss(false)
                    }

                    override fun onTargetCancel(view: TapTargetView?) {
                        mOneEducationHintOpen = false
                        saveEducationPreference(activity, saveEducationStringId)
                        super.onTargetCancel(view)
                    }
                })
                mOneEducationHintOpen = true
                doEducation = true
            } catch (e: Exception) {
                Log.w(Education::class.java.name, "Can't performed education " + e.message)
            }
        }
        return doEducation
    }

    /**
     * Define if educations screens are enabled
     */
    private fun isEducationScreensEnabled(): Boolean {
        return isEducationScreensEnabled(activity)
    }

    /**
     * Register education preferences as true in EDUCATION_PREFERENCE SharedPreferences
     *
     * @param context The context to retrieve the key string in XML
     * @param educationKeys Keys to save as boolean 'true'
     */
    private fun saveEducationPreference(context: Context, vararg educationKeys: Int) {
        val sharedPreferences = getEducationSharedPreferences(context)
        val editor = sharedPreferences.edit()
        for (key in educationKeys) {
            editor.putBoolean(context.getString(key), true)
        }
        editor.apply()
    }

    protected fun getCircleColor(): Int {
        val typedArray = activity.obtainStyledAttributes(intArrayOf(R.attr.colorPrimaryContainer))
        val colorControl = typedArray.getColor(0, Color.GREEN)
        typedArray.recycle()
        return colorControl
    }

    protected fun getCircleAlpha(): Float {
        return 0.98F
    }

    protected fun getTextColor(): Int {
        val typedArray = activity.obtainStyledAttributes(intArrayOf(R.attr.colorOnPrimaryContainer))
        val colorControl = typedArray.getColor(0, Color.WHITE)
        typedArray.recycle()
        return colorControl
    }

    companion object {

        private const val EDUCATION_PREFERENCE = "kdbxeducation"

        /**
         * All preference keys associated with education
         */
        val educationResourcesKeys = intArrayOf(
                R.string.education_create_db_key,
                R.string.education_select_db_key,
                R.string.education_unlock_key,
                R.string.education_read_only_key,
                R.string.education_biometric_key,
                R.string.education_search_key,
                R.string.education_new_node_key,
                R.string.education_sort_key,
                R.string.education_lock_key,
                R.string.education_copy_username_key,
                R.string.education_entry_edit_key,
                R.string.education_password_generator_key,
                R.string.education_entry_new_field_key,
                R.string.education_add_attachment_key,
                R.string.education_setup_OTP_key,
                R.string.education_validate_entry_key)

        fun putPropertiesInEducationPreferences(context: Context,
                                                editor: SharedPreferences.Editor,
                                                name: String,
                                                value: String) {
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
                context.getString(R.string.education_validate_entry_key) -> editor.putBoolean(name, value.toBoolean())
            }
        }

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
         * Determines whether the explanatory view of the biometric unlock has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_biometric_key key
         */
        fun isEducationBiometricPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_biometric_key),
                    context.resources.getBoolean(R.bool.education_biometric_default))
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
         * Determines whether the explanatory view of the new attachment button in an entry has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_add_attachment_key key
         */
        fun isEducationAddAttachmentPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_add_attachment_key),
                    context.resources.getBoolean(R.bool.education_add_attachment_default))
        }

        /**
         * Determines whether the explanatory view to setup OTP has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_setup_OTP_key key
         */
        fun isEducationSetupOTPPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_setup_OTP_key),
                    context.resources.getBoolean(R.bool.education_setup_OTP_default))
        }

        /**
         * Determines whether the explanatory view of the validate entry has already been displayed.
         *
         * @param context The context to open the SharedPreferences
         * @return boolean value of education_validate_entry_key key
         */
        fun isEducationValidateEntryPerformed(context: Context): Boolean {
            val prefs = getEducationSharedPreferences(context)
            return prefs.getBoolean(context.getString(R.string.education_validate_entry_key),
                context.resources.getBoolean(R.bool.education_validate_entry_default))
        }

        fun setEducationScreenReclickedPerformed(context: Context) {
            getEducationSharedPreferences(context)
                .edit()
                .putBoolean(context.getString(R.string.education_screen_reclicked_key), true)
                .apply()
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