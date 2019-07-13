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
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment
import com.kunzisoft.keepass.activities.dialogs.IconPickerDialogFragment.Companion.KEY_ICON_STANDARD
import com.kunzisoft.keepass.activities.lock.LockingHideActivity
import com.kunzisoft.keepass.view.EntryEditCustomField
import com.kunzisoft.keepass.app.App
import com.kunzisoft.keepass.database.action.ProgressDialogSaveDatabaseThread
import com.kunzisoft.keepass.database.action.node.ActionNodeValues
import com.kunzisoft.keepass.database.action.node.AddEntryRunnable
import com.kunzisoft.keepass.database.action.node.AfterActionNodeFinishRunnable
import com.kunzisoft.keepass.database.action.node.UpdateEntryRunnable
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.Util

class EntryEditActivity : LockingHideActivity(), IconPickerDialogFragment.IconPickerListener, GeneratePasswordDialogFragment.GeneratePasswordListener {

    private var mDatabase: Database? = null

    private var mEntry: EntryVersioned? = null
    private var mParent: GroupVersioned? = null
    private var mNewEntry: EntryVersioned? = null
    private var mIsNew: Boolean = false
    private var mSelectedIconStandard: PwIconStandard? = null

    // Views
    private var scrollView: ScrollView? = null
    private var entryTitleView: EditText? = null
    private var entryIconView: ImageView? = null
    private var entryUserNameView: EditText? = null
    private var entryUrlView: EditText? = null
    private var entryPasswordView: EditText? = null
    private var entryConfirmationPasswordView: EditText? = null
    private var generatePasswordView: View? = null
    private var entryCommentView: EditText? = null
    private var entryExtraFieldsContainer: ViewGroup? = null
    private var addNewFieldView: View? = null
    private var saveView: View? = null
    private var iconColor: Int = 0

    // View validation message
    private var validationErrorMessageId = UNKNOWN_MESSAGE

    // Education
    private var entryEditActivityEducation: EntryEditActivityEducation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.entry_edit)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        scrollView = findViewById(R.id.entry_edit_scroll)
        scrollView?.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET

        entryTitleView = findViewById(R.id.entry_edit_title)
        entryIconView = findViewById(R.id.entry_edit_icon_button)
        entryUserNameView = findViewById(R.id.entry_edit_user_name)
        entryUrlView = findViewById(R.id.entry_edit_url)
        entryPasswordView = findViewById(R.id.entry_edit_password)
        entryConfirmationPasswordView = findViewById(R.id.entry_edit_confirmation_password)
        entryCommentView = findViewById(R.id.entry_edit_notes)
        entryExtraFieldsContainer = findViewById(R.id.entry_edit_advanced_container)

        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(
                entryTitleView,
                entryIconView,
                entryUserNameView,
                entryUrlView,
                entryPasswordView,
                entryConfirmationPasswordView,
                entryCommentView,
                entryExtraFieldsContainer)

        // Likely the app has been killed exit the activity
        mDatabase = App.currentDatabase

        // Retrieve the textColor to tint the icon
        val taIconColor = theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        iconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()

        mSelectedIconStandard = mDatabase?.iconFactory?.unknownIcon

        // Entry is retrieve, it's an entry to update
        intent.getParcelableExtra<PwNodeId<*>>(KEY_ENTRY)?.let {
            mIsNew = false
            mEntry = mDatabase?.getEntryById(it)
            mEntry?.let { entry ->
                mParent = entry.parent
                fillEntryDataInContentsView(entry)
            }
        }

        // Parent is retrieve, it's a new entry to create
        intent.getParcelableExtra<PwNodeId<*>>(KEY_PARENT)?.let {
            mIsNew = true
            mEntry = mDatabase?.createEntry()
            mParent = mDatabase?.getGroupById(it)
            // Add the default icon
            mDatabase?.drawFactory?.assignDefaultDatabaseIconTo(this, entryIconView, iconColor)
        }

        // Close the activity if entry or parent can't be retrieve
        if (mEntry == null || mParent == null) {
            finish()
            return
        }

        // Assign title
        title = if (mIsNew) getString(R.string.add_entry) else getString(R.string.edit_entry)

        // Retrieve the icon after an orientation change
        savedInstanceState?.let {
            if (it.containsKey(KEY_ICON_STANDARD)) {
                iconPicked(it)
            }
        }

        // Add listener to the icon
        entryIconView?.setOnClickListener { IconPickerDialogFragment.launch(this@EntryEditActivity) }

        // Generate password button
        generatePasswordView = findViewById(R.id.entry_edit_generate_button)
        generatePasswordView?.setOnClickListener { openPasswordGenerator() }

        // Save button
        saveView = findViewById(R.id.entry_edit_save)
        mEntry?.let { entry ->
            saveView?.setOnClickListener { saveEntry(entry) }
        }

        if (mEntry?.allowExtraFields() == true) {
            addNewFieldView = findViewById(R.id.entry_edit_add_new_field)
            addNewFieldView?.apply {
                visibility = View.VISIBLE
                setOnClickListener { addNewCustomField() }
            }
        }

        // Verify the education views
        entryEditActivityEducation = EntryEditActivityEducation(this)
        entryEditActivityEducation?.let {
            Handler().post { performedNextEducation(it) }
        }
    }

    private fun performedNextEducation(entryEditActivityEducation: EntryEditActivityEducation) {
        if (generatePasswordView != null
                && entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                        generatePasswordView!!,
                        {
                            openPasswordGenerator()
                        },
                        {
                            performedNextEducation(entryEditActivityEducation)
                        }
                ))
        else if (mEntry != null
                && mEntry!!.allowExtraFields()
                && !mEntry!!.containsCustomFields()
                && addNewFieldView != null
                && entryEditActivityEducation.checkAndPerformedEntryNewFieldEducation(
                        addNewFieldView!!,
                        {
                            addNewCustomField()
                        }))
        ;
    }

    /**
     * Open the password generator fragment
     */
    private fun openPasswordGenerator() {
        GeneratePasswordDialogFragment().show(supportFragmentManager, "PasswordGeneratorFragment")
    }

    /**
     * Add a new view to fill in the information of the customized field
     */
    private fun addNewCustomField() {
        val entryEditCustomField = EntryEditCustomField(this@EntryEditActivity)
        entryEditCustomField.setData("", ProtectedString(false, ""))
        val visibilityFontActivated = PreferencesUtil.fieldFontIsInVisibility(this)
        entryEditCustomField.setFontVisibility(visibilityFontActivated)
        entryExtraFieldsContainer?.addView(entryEditCustomField)

        // Scroll bottom
        scrollView?.post { scrollView?.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    /**
     * Saves the new entry or update an existing entry in the database
     */
    private fun saveEntry(entry: EntryVersioned) {

        // Launch a validation and show the error if present
        if (!isValid()) {
            if (validationErrorMessageId != UNKNOWN_MESSAGE)
                Toast.makeText(this@EntryEditActivity, validationErrorMessageId, Toast.LENGTH_LONG).show()
            return
        }
        // Clone the entry
        mDatabase?.let { database ->
            mNewEntry = EntryVersioned(entry)
            mNewEntry?.let { newEntry ->
                populateEntryWithViewInfo(newEntry)

                // Open a progress dialog and save entry
                var actionRunnable: ActionRunnable? = null
                val afterActionNodeFinishRunnable = object : AfterActionNodeFinishRunnable() {
                    override fun onActionNodeFinish(actionNodeValues: ActionNodeValues) {
                        if (actionNodeValues.success)
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
                    actionRunnable = UpdateEntryRunnable(this@EntryEditActivity,
                            database,
                            entry,
                            newEntry,
                            afterActionNodeFinishRunnable,
                            !readOnly)
                }
                actionRunnable?.let { runnable ->
                    ProgressDialogSaveDatabaseThread(this@EntryEditActivity) {runnable}.start()
                }
            }
        }
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    private fun isValid(): Boolean {

        // Require title
        if (entryTitleView?.text.toString().isEmpty()) {
            validationErrorMessageId = R.string.error_title_required
            return false
        }

        // Validate password
        if (entryPasswordView?.text.toString() != entryConfirmationPasswordView?.text.toString()) {
            validationErrorMessageId = R.string.error_pass_match
            return false
        }

        // Validate extra fields
        if (mEntry?.allowExtraFields() == true) {
            entryExtraFieldsContainer?.let {
                for (i in 0 until it.childCount) {
                    val entryEditCustomField = it.getChildAt(i) as EntryEditCustomField
                    val key = entryEditCustomField.label
                    if (key == null || key.isEmpty()) {
                        validationErrorMessageId = R.string.error_string_key
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun populateEntryWithViewInfo(newEntry: EntryVersioned) {

        mDatabase?.startManageEntry(newEntry)

        newEntry.lastAccessTime = PwDate()
        newEntry.lastModificationTime = PwDate()

        newEntry.title = entryTitleView?.text.toString()
        newEntry.icon = retrieveIcon()

        newEntry.url = entryUrlView?.text.toString()
        newEntry.username = entryUserNameView?.text.toString()
        newEntry.notes = entryCommentView?.text.toString()
        newEntry.password = entryPasswordView?.text.toString()

        if (newEntry.allowExtraFields()) {
            // Delete all extra strings
            newEntry.removeAllCustomFields()
            // Add extra fields from views
            entryExtraFieldsContainer?.let {
                for (i in 0 until it.childCount) {
                    val view = it.getChildAt(i) as EntryEditCustomField
                    val key = view.label
                    val value = view.value
                    val protect = view.isProtected
                    newEntry.addExtraField(key, ProtectedString(protect, value))
                }
            }
        }

        mDatabase?.stopManageEntry(newEntry)
    }

    /**
     * Retrieve the icon by the selection, or the first icon in the list if the entry is new or the last one
     */
    private fun retrieveIcon(): PwIcon {

        return if (mSelectedIconStandard?.isUnknown != true)
            mSelectedIconStandard
        else {
            if (mIsNew) {
                mDatabase?.iconFactory?.keyIcon
            } else {
                // Keep previous icon, if no new one was selected
                mEntry?.icon
            }
        } ?: PwIconStandard()
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

    private fun assignIconView() {
        mEntry?.icon?.let {
            mDatabase?.drawFactory?.assignDatabaseIconTo(
                    this,
                    entryIconView,
                    it,
                    iconColor)
        }
    }

    private fun fillEntryDataInContentsView(entry: EntryVersioned) {

        assignIconView()

        // Don't start the field reference manager, we want to see the raw ref
        mDatabase?.stopManageEntry(entry)

        entryTitleView?.setText(entry.title)
        entryUserNameView?.setText(entry.username)
        entryUrlView?.setText(entry.url)
        val password = entry.password
        entryPasswordView?.setText(password)
        entryConfirmationPasswordView?.setText(password)
        entryCommentView?.setText(entry.notes)

        val visibilityFontActivated = PreferencesUtil.fieldFontIsInVisibility(this)
        if (visibilityFontActivated) {
            Util.applyFontVisibilityTo(this, entryUserNameView)
            Util.applyFontVisibilityTo(this, entryPasswordView)
            Util.applyFontVisibilityTo(this, entryConfirmationPasswordView)
            Util.applyFontVisibilityTo(this, entryCommentView)
        }

        if (entry.allowExtraFields()) {
            val container = findViewById<LinearLayout>(R.id.entry_edit_advanced_container)
            entry.fields.doActionToAllCustomProtectedField { key, value ->
                val entryEditCustomField = EntryEditCustomField(this@EntryEditActivity)
                entryEditCustomField.setData(key, value)
                entryEditCustomField.setFontVisibility(visibilityFontActivated)
                container.addView(entryEditCustomField)
            }
        }
    }

    override fun iconPicked(bundle: Bundle) {
        mSelectedIconStandard = bundle.getParcelable(KEY_ICON_STANDARD)
        mSelectedIconStandard?.let {
            mEntry?.icon = it
        }
        assignIconView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!mSelectedIconStandard!!.isUnknown) {
            outState.putParcelable(KEY_ICON_STANDARD, mSelectedIconStandard)
            super.onSaveInstanceState(outState)
        }
    }

    override fun acceptPassword(bundle: Bundle) {
        bundle.getString(GeneratePasswordDialogFragment.KEY_PASSWORD_ID)?.let {
            entryPasswordView?.setText(it)
            entryConfirmationPasswordView?.setText(it)
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

        // Keys for callback
        const val ADD_ENTRY_RESULT_CODE = 31
        const val UPDATE_ENTRY_RESULT_CODE = 32
        const val ADD_OR_UPDATE_ENTRY_REQUEST_CODE = 7129
        const val ADD_OR_UPDATE_ENTRY_KEY = "ADD_OR_UPDATE_ENTRY_KEY"

        const val UNKNOWN_MESSAGE = -1

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
