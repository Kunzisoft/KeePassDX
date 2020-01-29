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
 */
package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SetOTPDialogFragment
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.notifications.ClipboardEntryNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.notifications.KeyboardEntryNotificationService
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.EntryEditContentsView
import com.kunzisoft.keepass.view.asError
import java.util.*

class EntryEditActivity : LockingActivity(),
        IconPickerDialogFragment.IconPickerListener,
        GeneratePasswordDialogFragment.GeneratePasswordListener,
        SetOTPDialogFragment.CreateOtpListener {

    private var mDatabase: Database? = null

    // Refs of an entry and group in database, are not modifiable
    private var mEntry: Entry? = null
    private var mParent: Group? = null
    // New or copy of mEntry in the database to be modifiable
    private var mNewEntry: Entry? = null
    private var mIsNew: Boolean = false

    // Views
    private var coordinatorLayout: CoordinatorLayout? = null
    private var scrollView: ScrollView? = null
    private var entryEditContentsView: EntryEditContentsView? = null
    private var saveView: View? = null

    // Education
    private var entryEditActivityEducation: EntryEditActivityEducation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_edit)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        coordinatorLayout = findViewById(R.id.entry_edit_coordinator_layout)

        scrollView = findViewById(R.id.entry_edit_scroll)
        scrollView?.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET

        entryEditContentsView = findViewById(R.id.entry_edit_contents)
        entryEditContentsView?.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this))
        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(entryEditContentsView)

        stopService(Intent(this, ClipboardEntryNotificationService::class.java))
        stopService(Intent(this, KeyboardEntryNotificationService::class.java))

        // Likely the app has been killed exit the activity
        mDatabase = Database.getInstance()

        // Entry is retrieve, it's an entry to update
        intent.getParcelableExtra<NodeId<UUID>>(KEY_ENTRY)?.let {
            mIsNew = false
            // Create an Entry copy to modify from the database entry
            mEntry = mDatabase?.getEntryById(it)

            // Retrieve the parent
            mEntry?.let { entry ->
                mParent = entry.parent
                // If no parent, add root group as parent
                if (mParent == null) {
                    mParent = mDatabase?.rootGroup
                    entry.parent = mParent
                }
            }

            // Create the new entry from the current one
            if (savedInstanceState == null
                    || !savedInstanceState.containsKey(KEY_NEW_ENTRY)) {
                mEntry?.let { entry ->
                    // Create a copy to modify
                    mNewEntry = Entry(entry).also { newEntry ->
                        // WARNING Remove the parent to keep memory with parcelable
                        newEntry.removeParent()
                    }
                }
            }
        }

        // Parent is retrieve, it's a new entry to create
        intent.getParcelableExtra<NodeId<*>>(KEY_PARENT)?.let {
            mIsNew = true
            // Create an empty new entry
            if (savedInstanceState == null
                    || !savedInstanceState.containsKey(KEY_NEW_ENTRY)) {
                mNewEntry = mDatabase?.createEntry()
            }
            mParent = mDatabase?.getGroupById(it)
            // Add the default icon
            mDatabase?.drawFactory?.let { iconFactory ->
                entryEditContentsView?.setDefaultIcon(iconFactory)
            }
        }

        // Retrieve the new entry after an orientation change
        if (savedInstanceState != null
                && savedInstanceState.containsKey(KEY_NEW_ENTRY)) {
            mNewEntry = savedInstanceState.getParcelable(KEY_NEW_ENTRY)
        }

        // Close the activity if entry or parent can't be retrieve
        if (mNewEntry == null || mParent == null) {
            finish()
            return
        }

        populateViewsWithEntry(mNewEntry!!)

        // Assign title
        title = if (mIsNew) getString(R.string.add_entry) else getString(R.string.edit_entry)

        // Add listener to the icon
        entryEditContentsView?.setOnIconViewClickListener { IconPickerDialogFragment.launch(this@EntryEditActivity) }

        // Generate password button
        entryEditContentsView?.setOnPasswordGeneratorClickListener { openPasswordGenerator() }

        // Save button
        saveView = findViewById(R.id.entry_edit_save)
        saveView?.setOnClickListener { saveEntry() }

        entryEditContentsView?.allowCustomField(mNewEntry?.allowCustomFields() == true) {
            addNewCustomField()
        }

        // Verify the education views
        entryEditActivityEducation = EntryEditActivityEducation(this)

        // Create progress dialog
        mProgressDialogThread?.onActionFinish = { actionTask, result ->
            when (actionTask) {
                ACTION_DATABASE_CREATE_ENTRY_TASK,
                ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                    if (result.isSuccess)
                        finish()
                }
            }

            // Show error
            if (!result.isSuccess) {
                result.message?.let { resultMessage ->
                    Snackbar.make(coordinatorLayout!!,
                            resultMessage,
                            Snackbar.LENGTH_LONG).asError().show()
                }
            }
        }
    }

    private fun populateViewsWithEntry(newEntry: Entry) {
        // Don't start the field reference manager, we want to see the raw ref
        mDatabase?.stopManageEntry(newEntry)

        // Set info in temp parameters
        temporarilySaveAndShowSelectedIcon(newEntry.icon)

        // Set info in view
        entryEditContentsView?.apply {
            title = newEntry.title
            username = if (newEntry.username.isEmpty()) mDatabase?.defaultUsername ?:"" else newEntry.username
            url = newEntry.url
            password = newEntry.password
            notes = newEntry.notes
            for (entry in newEntry.customFields.entries) {
                post {
                    putCustomField(entry.key, entry.value)
                }
            }
        }
    }

    private fun populateEntryWithViews(newEntry: Entry) {

        mDatabase?.startManageEntry(newEntry)

        newEntry.apply {
            // Build info from view
            entryEditContentsView?.let { entryView ->
                removeAllFields()
                title = entryView.title
                username = entryView.username
                url = entryView.url
                password = entryView.password
                notes = entryView.notes
                entryView.customFields.forEach { customField ->
                    putExtraField(customField.name, customField.protectedValue)
                }
            }
        }

        mDatabase?.stopManageEntry(newEntry)
    }

    private fun temporarilySaveAndShowSelectedIcon(icon: IconImage) {
        mNewEntry?.icon = icon
        mDatabase?.drawFactory?.let { iconDrawFactory ->
            entryEditContentsView?.setIcon(iconDrawFactory, icon)
        }
    }

    /**
     * Open the password generator fragment
     */
    private fun openPasswordGenerator() {
        GeneratePasswordDialogFragment().show(supportFragmentManager, "PasswordGeneratorFragment")
    }

    /**
     * Add a new customized field view and scroll to bottom
     */
    private fun addNewCustomField() {
        entryEditContentsView?.addEmptyCustomField()
    }

    /**
     * Saves the new entry or update an existing entry in the database
     */
    private fun saveEntry() {

        // Launch a validation and show the error if present
        if (entryEditContentsView?.isValid() == true) {
            // Clone the entry
            mNewEntry?.let { newEntry ->

                // WARNING Add the parent previously deleted
                newEntry.parent = mEntry?.parent
                // Build info
                newEntry.lastAccessTime = DateInstant()
                newEntry.lastModificationTime = DateInstant()

                populateEntryWithViews(newEntry)

                // Open a progress dialog and save entry
                if (mIsNew) {
                    mParent?.let { parent ->
                        mProgressDialogThread?.startDatabaseCreateEntry(
                                newEntry,
                                parent,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                } else {
                    mEntry?.let { oldEntry ->
                        mProgressDialogThread?.startDatabaseUpdateEntry(
                                oldEntry,
                                newEntry,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater = menuInflater
        inflater.inflate(R.menu.database, menu)
        // Save database not needed here
        menu.findItem(R.id.menu_save_database)?.isVisible = false
        MenuUtil.contributionMenuInflater(inflater, menu)
        if (mDatabase?.allowOTP == true)
            inflater.inflate(R.menu.entry_otp, menu)

        entryEditActivityEducation?.let {
            Handler().post { performedNextEducation(it) }
        }

        return true
    }

    private fun performedNextEducation(entryEditActivityEducation: EntryEditActivityEducation) {
        val passwordView = entryEditContentsView?.generatePasswordView
        val addNewFieldView = entryEditContentsView?.addNewFieldButton

        val generatePasswordEducationPerformed = passwordView != null
                && entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                passwordView,
                {
                    openPasswordGenerator()
                },
                {
                    performedNextEducation(entryEditActivityEducation)
                }
        )
        if (!generatePasswordEducationPerformed) {
            // entryNewFieldEducationPerformed
            mNewEntry != null && mNewEntry!!.allowCustomFields() && mNewEntry!!.customFields.isEmpty()
                    && addNewFieldView != null && addNewFieldView.visibility == View.VISIBLE
                    && entryEditActivityEducation.checkAndPerformedEntryNewFieldEducation(
                    addNewFieldView,
                    {
                        addNewCustomField()
                    })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_lock -> {
                lockAndExit()
                return true
            }
            R.id.menu_save_database -> {
                mProgressDialogThread?.startDatabaseSave(!mReadOnly)
            }
            R.id.menu_contribute -> {
                MenuUtil.onContributionItemSelected(this)
                return true
            }
            R.id.menu_add_otp -> {
                // Retrieve the current otpElement if exists
                // and open the dialog to set up the OTP
                SetOTPDialogFragment.build(mEntry?.getOtpElement()?.otpModel)
                        .show(supportFragmentManager, "addOTPDialog")
                return true
            }
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onOtpCreated(otpElement: OtpElement) {
        // Update the otp field with otpauth:// url
        val otpField = OtpEntryFields.buildOtpField(otpElement,
                mEntry?.title, mEntry?.username)
        entryEditContentsView?.putCustomField(otpField.name, otpField.protectedValue)
        mEntry?.putExtraField(otpField.name, otpField.protectedValue)
    }

    override fun iconPicked(bundle: Bundle) {
        IconPickerDialogFragment.getIconStandardFromBundle(bundle)?.let { icon ->
            temporarilySaveAndShowSelectedIcon(icon)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mNewEntry?.let {
            populateEntryWithViews(it)
            outState.putParcelable(KEY_NEW_ENTRY, it)
        }

        super.onSaveInstanceState(outState)
    }

    override fun acceptPassword(bundle: Bundle) {
        bundle.getString(GeneratePasswordDialogFragment.KEY_PASSWORD_ID)?.let {
            entryEditContentsView?.password = it
        }

        entryEditActivityEducation?.let {
            Handler().post { performedNextEducation(it) }
        }
    }

    override fun cancelPassword(bundle: Bundle) {
        // Do nothing here
    }

    override fun finish() {
        // Assign entry callback as a result in all case
        try {
            mNewEntry?.let {
                val bundle = Bundle()
                val intentEntry = Intent()
                bundle.putParcelable(ADD_OR_UPDATE_ENTRY_KEY, mNewEntry)
                intentEntry.putExtras(bundle)
                if (mIsNew) {
                    setResult(ADD_ENTRY_RESULT_CODE, intentEntry)
                } else {
                    setResult(UPDATE_ENTRY_RESULT_CODE, intentEntry)
                }
            }
            super.finish()
        } catch (e: Exception) {
            // Exception when parcelable can't be done
            Log.e(TAG, "Cant add entry as result", e)
        }
    }

    companion object {

        private val TAG = EntryEditActivity::class.java.name

        // Keys for current Activity
        const val KEY_ENTRY = "entry"
        const val KEY_PARENT = "parent"

        // SaveInstanceState
        const val KEY_NEW_ENTRY = "new_entry"

        // Keys for callback
        const val ADD_ENTRY_RESULT_CODE = 31
        const val UPDATE_ENTRY_RESULT_CODE = 32
        const val ADD_OR_UPDATE_ENTRY_REQUEST_CODE = 7129
        const val ADD_OR_UPDATE_ENTRY_KEY = "ADD_OR_UPDATE_ENTRY_KEY"

        /**
         * Launch EntryEditActivity to update an existing entry
         *
         * @param activity from activity
         * @param pwEntry Entry to update
         */
        fun launch(activity: Activity, pwEntry: Entry) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_ENTRY, pwEntry.nodeId)
                activity.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry
         *
         * @param activity from activity
         * @param pwGroup Group who will contains new entry
         */
        fun launch(activity: Activity, pwGroup: Group) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, pwGroup.nodeId)
                activity.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }
    }
}
