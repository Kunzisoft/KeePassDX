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
 */
package com.kunzisoft.keepass.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.activities.lock.LockingHideActivity
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.action.ProgressDialogSaveDatabaseThread
import com.kunzisoft.keepass.database.action.node.ActionNodeValues
import com.kunzisoft.keepass.database.action.node.AddEntryRunnable
import com.kunzisoft.keepass.database.action.node.AfterActionNodeFinishRunnable
import com.kunzisoft.keepass.database.action.node.UpdateEntryRunnable
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.EntryEditContentsView

class EntryEditActivity : LockingHideActivity(), IconPickerDialogFragment.IconPickerListener, GeneratePasswordDialogFragment.GeneratePasswordListener {

    private var mDatabase: Database? = null

    // Refs of an entry and group in database, are not modifiable
    private var mEntry: EntryVersioned? = null
    private var mParent: GroupVersioned? = null
    // New or copy of mEntry in the database to be modifiable
    private var mNewEntry: EntryVersioned? = null
    private var mIsNew: Boolean = false

    // Views
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

        scrollView = findViewById(R.id.entry_edit_scroll)
        scrollView?.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET

        entryEditContentsView = findViewById(R.id.entry_edit_contents)
        entryEditContentsView?.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this))
        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(entryEditContentsView)

        // Likely the app has been killed exit the activity
        mDatabase = App.currentDatabase

        // Entry is retrieve, it's an entry to update
        intent.getParcelableExtra<PwNodeId<*>>(KEY_ENTRY)?.let {
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

            // Retrieve the icon after an orientation change
            if (savedInstanceState != null && savedInstanceState.containsKey(KEY_NEW_ENTRY)) {
                mNewEntry = savedInstanceState.getParcelable(KEY_NEW_ENTRY) as EntryVersioned
            } else {
                mEntry?.let { entry ->
                    // Create a copy to modify
                    mNewEntry = EntryVersioned(entry).also { newEntry ->

                        // WARNING Remove the parent to keep memory with parcelable
                        newEntry.parent = null
                    }
                }
            }
        }

        // Parent is retrieve, it's a new entry to create
        intent.getParcelableExtra<PwNodeId<*>>(KEY_PARENT)?.let {
            mIsNew = true
            mNewEntry = mDatabase?.createEntry()
            mParent = mDatabase?.getGroupById(it)
            // Add the default icon
            mDatabase?.drawFactory?.let { iconFactory ->
                entryEditContentsView?.setDefaultIcon(iconFactory)
            }
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

        entryEditContentsView?.allowCustomField(mNewEntry?.allowExtraFields() == true) { addNewCustomField() }

        // Verify the education views
        entryEditActivityEducation = EntryEditActivityEducation(this)
        entryEditActivityEducation?.let {
            Handler().post { performedNextEducation(it) }
        }
    }

    private fun performedNextEducation(entryEditActivityEducation: EntryEditActivityEducation) {
        val passwordView = entryEditContentsView?.generatePasswordView
        val addNewFieldView = entryEditContentsView?.addNewFieldView

        if (passwordView != null
                && entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                        passwordView,
                        {
                            openPasswordGenerator()
                        },
                        {
                            performedNextEducation(entryEditActivityEducation)
                        }
                ))
        else if (mNewEntry != null && mNewEntry!!.allowExtraFields() && !mNewEntry!!.containsCustomFields()
                && addNewFieldView != null && addNewFieldView.visibility == View.VISIBLE
                && entryEditActivityEducation.checkAndPerformedEntryNewFieldEducation(
                        addNewFieldView,
                        {
                            addNewCustomField()
                        }))
            ;
    }

    private fun populateViewsWithEntry(newEntry: EntryVersioned) {
        // Don't start the field reference manager, we want to see the raw ref
        mDatabase?.stopManageEntry(newEntry)

        // Set info in temp parameters
        temporarilySaveAndShowSelectedIcon(newEntry.icon)

        // Set info in view
        entryEditContentsView?.apply {
            title = newEntry.title
            username = newEntry.username
            url = newEntry.url
            password = newEntry.password
            notes = newEntry.notes
            newEntry.fields.doActionToAllCustomProtectedField { key, value ->
                addNewCustomField(key, value)
            }
        }
    }

    private fun populateEntryWithViews(newEntry: EntryVersioned) {

        mDatabase?.startManageEntry(newEntry)

        newEntry.apply {
            // Build info from view
            entryEditContentsView?.let { entryView ->
                title = entryView.title
                username = entryView.username
                url = entryView.url
                password = entryView.password
                notes = entryView.notes
                entryView.customFields.forEach { customField ->
                    addExtraField(customField.name, customField.protectedValue)
                }
            }
        }

        mDatabase?.stopManageEntry(newEntry)
    }

    private fun temporarilySaveAndShowSelectedIcon(icon: PwIcon) {
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
        entryEditContentsView?.addNewCustomField()
        // Scroll bottom
        scrollView?.post { scrollView?.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    /**
     * Saves the new entry or update an existing entry in the database
     */
    private fun saveEntry() {

        // Launch a validation and show the error if present
        if (entryEditContentsView?.isValid() == true) {
            // Clone the entry
            mDatabase?.let { database ->
                mNewEntry?.let { newEntry ->

                    // WARNING Add the parent previously deleted
                    newEntry.parent = mEntry?.parent
                    // Build info
                    newEntry.lastAccessTime = PwDate()
                    newEntry.lastModificationTime = PwDate()

                    populateEntryWithViews(newEntry)

                    // Open a progress dialog and save entry
                    var actionRunnable: ActionRunnable? = null
                    val afterActionNodeFinishRunnable = object : AfterActionNodeFinishRunnable() {
                        override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
                            if (actionNodeValues.result.isSuccess)
                                finish()
                        }
                    }
                    if (mIsNew) {
                        mParent?.let { parent ->
                            actionRunnable = AddEntryRunnable(this@EntryEditActivity,
                                    database,
                                    newEntry,
                                    parent,
                                    afterActionNodeFinishRunnable,
                                    !readOnly)
                        }

                    } else {
                        mEntry?.let { oldEntry ->
                            actionRunnable = UpdateEntryRunnable(this@EntryEditActivity,
                                    database,
                                    oldEntry,
                                    newEntry,
                                    afterActionNodeFinishRunnable,
                                    !readOnly)
                        }
                    }
                    actionRunnable?.let { runnable ->
                        ProgressDialogSaveDatabaseThread(this@EntryEditActivity) { runnable }.start()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater = menuInflater
        inflater.inflate(R.menu.database_lock, menu)
        MenuUtil.contributionMenuInflater(inflater, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_lock -> {
                lockAndExit()
                return true
            }

            R.id.menu_contribute -> return MenuUtil.onContributionItemSelected(this)

            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun iconPicked(bundle: Bundle) {
        IconPickerDialogFragment.getIconStandardFromBundle(bundle)?.let { icon ->
            temporarilySaveAndShowSelectedIcon(icon)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_NEW_ENTRY, mNewEntry)

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
        fun launch(activity: Activity, pwEntry: EntryVersioned) {
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
        fun launch(activity: Activity, pwGroup: GroupVersioned) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, pwGroup.nodeId)
                activity.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }
    }
}
