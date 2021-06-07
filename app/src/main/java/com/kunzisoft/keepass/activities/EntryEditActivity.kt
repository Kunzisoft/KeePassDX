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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.*
import com.kunzisoft.keepass.activities.dialogs.FileTooBigDialogFragment.Companion.MAX_WARNING_BINARY_FILE
import com.kunzisoft.keepass.activities.fragments.EntryEditFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.adapters.TemplatesSelectorAdapter
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.*
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.services.AttachmentFileNotificationService
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RELOAD_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

class EntryEditActivity : LockingActivity(),
        EntryCustomFieldDialogFragment.EntryCustomFieldListener,
        GeneratePasswordDialogFragment.GeneratePasswordListener,
        SetOTPDialogFragment.CreateOtpListener,
        DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener,
        FileTooBigDialogFragment.ActionChooseListener,
        ReplaceFileDialogFragment.ActionChooseListener {

    // Refs of an entry and group in database, are not modifiable
    private var mEntry: Entry? = null
    private var mParent: Group? = null
    private var mIsNew: Boolean = false
    private var mIsTemplate: Boolean = false
    private var mEntryTemplate: Template? = null

    // Views
    private var coordinatorLayout: CoordinatorLayout? = null
    private var scrollView: NestedScrollView? = null
    private var templateSelectorSpinner: Spinner? = null
    private var entryEditFragment: EntryEditFragment? = null
    private var entryEditAddToolBar: ToolbarAction? = null
    private var validateButton: View? = null
    private var lockView: View? = null
    private var loadingView: ProgressBar? = null

    // To manage attachments
    private var mExternalFileHelper: ExternalFileHelper? = null
    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    private var mAllowMultipleAttachments: Boolean = false
    private var mTempAttachments = ArrayList<EntryAttachmentState>()

    // Education
    private var entryEditActivityEducation: EntryEditActivityEducation? = null

    // To ask data lost only one time
    private var backPressedAlreadyApproved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_edit)

        // Bottom Bar
        entryEditAddToolBar = findViewById(R.id.entry_edit_bottom_bar)
        setSupportActionBar(entryEditAddToolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        coordinatorLayout = findViewById(R.id.entry_edit_coordinator_layout)

        scrollView = findViewById(R.id.entry_edit_scroll)
        scrollView?.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET

        lockView = findViewById(R.id.lock_button)
        lockView?.setOnClickListener {
            lockAndExit()
        }

        loadingView = findViewById(R.id.loading)

        // Focus view to reinitialize timeout
        coordinatorLayout?.resetAppTimeoutWhenViewFocusedOrChanged(this, mDatabase)

        stopService(Intent(this, ClipboardEntryNotificationService::class.java))
        stopService(Intent(this, KeyboardEntryNotificationService::class.java))

        var tempEntryInfo: EntryInfo? = null

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
            checkIfTemplate()
            tempEntryInfo = mEntry?.getEntryInfo(mDatabase, true)
        }

        // Parent is retrieve, it's a new entry to create
        intent.getParcelableExtra<NodeId<*>>(KEY_PARENT)?.let {
            mIsNew = true
            mParent = mDatabase?.getGroupById(it)
            // Add the default icon from parent if not a folder
            val parentIcon = mParent?.icon
            mEntry = mDatabase?.createEntry()
            checkIfTemplate()
            tempEntryInfo = mEntry?.getEntryInfo(mDatabase, true)
            // Set default icon
            if (parentIcon != null) {
                if (parentIcon.custom.isUnknown
                        && parentIcon.standard.id != IconImageStandard.FOLDER_ID) {
                    tempEntryInfo?.icon = IconImage(parentIcon.standard)
                }
                if (!parentIcon.custom.isUnknown) {
                    tempEntryInfo?.icon = IconImage(parentIcon.custom)
                }
            }
            // Set default username
            tempEntryInfo?.username = mDatabase?.defaultUsername ?: ""
        }

        // Retrieve data from registration
        val registerInfo = EntrySelectionHelper.retrieveRegisterInfoFromIntent(intent)
        val searchInfo: SearchInfo? = registerInfo?.searchInfo
                ?: EntrySelectionHelper.retrieveSearchInfoFromIntent(intent)

        searchInfo?.let { tempSearchInfo ->
            tempEntryInfo?.saveSearchInfo(mDatabase, tempSearchInfo)
        }

        registerInfo?.let { regInfo ->
            tempEntryInfo?.saveRegisterInfo(mDatabase, regInfo)
        }

        // Build fragment to manage entry modification
        entryEditFragment = supportFragmentManager.findFragmentByTag(ENTRY_EDIT_FRAGMENT_TAG) as? EntryEditFragment?
        if (entryEditFragment == null) {
            entryEditFragment = EntryEditFragment.getInstance(tempEntryInfo, mEntryTemplate)
        }
        entryEditFragment?.apply {
            drawFactory = mDatabase?.iconDrawableFactory
            onDateTimeClickListener = { dateInstant ->
                if (dateInstant.type == DateInstant.Type.TIME) {
                    selectTime(dateInstant)
                } else {
                    selectDate(dateInstant)
                }
            }
            onPasswordGeneratorClickListener = { field ->
                GeneratePasswordDialogFragment
                        .getInstance(field)
                        .show(supportFragmentManager, "PasswordGeneratorFragment")
            }
            // Add listener to the icon
            onIconClickListener = { iconImage ->
                IconPickerActivity.launch(this@EntryEditActivity, iconImage)
            }
            onRemoveAttachment = { attachment ->
                mAttachmentFileBinderManager?.removeBinaryAttachment(attachment)
                removeAttachment(EntryAttachmentState(attachment, StreamDirection.DOWNLOAD))
            }
            onEditCustomFieldClickListener = { field ->
                editCustomField(field)
            }
        }

        // To show Fragment asynchronously
        lifecycleScope.launchWhenResumed {
            loadingView?.hideByFading()
            entryEditFragment?.let { fragment ->
                supportFragmentManager.beginTransaction()
                        .replace(R.id.entry_edit_content, fragment, ENTRY_EDIT_FRAGMENT_TAG)
                        .commit()
            }
        }

        // Change template dynamically
        templateSelectorSpinner = findViewById(R.id.entry_edit_template_selector)
        templateSelectorSpinner?.apply {
            // Build template selector
            val templates = mDatabase?.getTemplates(mIsTemplate)
            if (templates != null && templates.isNotEmpty()) {
                adapter = TemplatesSelectorAdapter(this@EntryEditActivity, mDatabase, templates)
                setSelection(templates.indexOf(mEntryTemplate))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val newTemplate = templates[position]
                        entryEditFragment?.apply {
                            if (getTemplate() != newTemplate) {
                                assignTemplate(newTemplate)
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            } else {
                visibility = View.GONE
            }
        }

        // Retrieve temp attachments in case of deletion
        if (savedInstanceState?.containsKey(TEMP_ATTACHMENTS) == true) {
            mTempAttachments = savedInstanceState.getParcelableArrayList(TEMP_ATTACHMENTS) ?: mTempAttachments
        }

        // To retrieve attachment
        mExternalFileHelper = ExternalFileHelper(this)
        mAttachmentFileBinderManager = AttachmentFileBinderManager(this)

        // Save button
        validateButton = findViewById(R.id.entry_edit_validate)
        validateButton?.setOnClickListener { saveEntry() }

        // Verify the education views
        entryEditActivityEducation = EntryEditActivityEducation(this)

        // Create progress dialog
        mProgressDatabaseTaskProvider?.onActionFinish = { actionTask, result ->
            when (actionTask) {
                ACTION_DATABASE_CREATE_ENTRY_TASK,
                ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                    try {
                        if (result.isSuccess) {
                            var newNodes: List<Node> = ArrayList()
                            result.data?.getBundle(DatabaseTaskNotificationService.NEW_NODES_KEY)?.let { newNodesBundle ->
                                mDatabase?.let { database ->
                                    newNodes = DatabaseTaskNotificationService.getListNodesFromBundle(database, newNodesBundle)
                                }
                            }
                            if (newNodes.size == 1) {
                                (newNodes[0] as? Entry?)?.let { entry ->
                                    mEntry = entry
                                    EntrySelectionHelper.doSpecialAction(intent,
                                            {
                                                // Finish naturally
                                                finishForEntryResult()
                                            },
                                            {
                                                // Nothing when search retrieved
                                            },
                                            {
                                                entryValidatedForSave()
                                            },
                                            {
                                                entryValidatedForKeyboardSelection(entry)
                                            },
                                            { _, _ ->
                                                entryValidatedForAutofillSelection(entry)
                                            },
                                            {
                                                entryValidatedForAutofillRegistration()
                                            }
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve entry after database action", e)
                    }
                }
                ACTION_DATABASE_RELOAD_TASK -> {
                    // Close the current activity
                    this.showActionErrorIfNeeded(result)
                    finish()
                }
            }
            coordinatorLayout?.showActionErrorIfNeeded(result)
        }
    }

    private fun checkIfTemplate() {
        // Define is current entry is a template (in direct template group)
        mIsTemplate = mDatabase?.entryIsTemplate(mEntry) ?: false

        val templates = mDatabase?.getTemplates(mIsTemplate)
        mEntryTemplate = mEntry?.let {
            mDatabase?.getTemplate(it)
        } ?: if (templates?.isNotEmpty() == true) Template.STANDARD else null

        // Decode the entry
        mEntry?.let {
            mEntry = mDatabase?.decodeEntryWithTemplateConfiguration(it)
        }
    }

    private fun entryValidatedForSave() {
        onValidateSpecialMode()
        finishForEntryResult()
    }

    private fun entryValidatedForKeyboardSelection(entry: Entry) {
        // Populate Magikeyboard with entry
        mDatabase?.let { database ->
            populateKeyboardAndMoveAppToBackground(this,
                    entry.getEntryInfo(database),
                    intent)
        }
        onValidateSpecialMode()
        // Don't keep activity history for entry edition
        finishForEntryResult()
    }

    private fun entryValidatedForAutofillSelection(entry: Entry) {
        // Build Autofill response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mDatabase?.let { database ->
                AutofillHelper.buildResponseAndSetResult(this@EntryEditActivity,
                        database,
                        entry.getEntryInfo(database))
            }
        }
        onValidateSpecialMode()
    }

    private fun entryValidatedForAutofillRegistration() {
        onValidateSpecialMode()
        finishForEntryResult()
    }

    override fun onResume() {
        super.onResume()

        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Padding if lock button visible
        entryEditAddToolBar?.updateLockPaddingLeft()

        mAllowMultipleAttachments = mDatabase?.allowMultipleAttachments == true
        mAttachmentFileBinderManager?.apply {
            registerProgressTask()
            onActionTaskListener = object : AttachmentFileNotificationService.ActionTaskListener {
                override fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState) {
                    when (entryAttachmentState.downloadState) {
                        AttachmentState.START -> {
                            entryEditFragment?.apply {
                                putAttachment(entryAttachmentState)
                                // Scroll to the attachment position
                                getAttachmentViewPosition(entryAttachmentState) {
                                    scrollView?.smoothScrollTo(0, it.toInt())
                                }
                            }            // Add in temp list
                            mTempAttachments.add(entryAttachmentState)
                        }
                        AttachmentState.IN_PROGRESS -> {
                            entryEditFragment?.putAttachment(entryAttachmentState)
                        }
                        AttachmentState.COMPLETE -> {
                            entryEditFragment?.putAttachment(entryAttachmentState) {
                                entryEditFragment?.getAttachmentViewPosition(entryAttachmentState) {
                                    scrollView?.smoothScrollTo(0, it.toInt())
                                }
                            }
                        }
                        AttachmentState.CANCELED -> {
                            entryEditFragment?.removeAttachment(entryAttachmentState)
                        }
                        AttachmentState.ERROR -> {
                            entryEditFragment?.removeAttachment(entryAttachmentState)
                            coordinatorLayout?.let {
                                Snackbar.make(it, R.string.error_file_not_create, Snackbar.LENGTH_LONG).asError().show()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onPause() {
        mAttachmentFileBinderManager?.unregisterProgressTask()

        super.onPause()
    }

    /**
     * Add a new customized field
     */
    private fun addNewCustomField() {
        //if (mIsTemplate) {
            // TODO Custom Dialog to add a complete template field
        //} else {
            EntryCustomFieldDialogFragment.getInstance().show(supportFragmentManager, "customFieldDialog")
        //}
    }

    private fun editCustomField(field: Field) {
        //if (mIsTemplate) {
        // TODO Custom Dialog to edit a complete template field
        //} else {
            EntryCustomFieldDialogFragment.getInstance(field).show(supportFragmentManager, "customFieldDialog")
        //}
    }

    private fun showAddCustomFieldError() {
        coordinatorLayout?.let {
            Snackbar.make(it, R.string.error_field_name_already_exists, Snackbar.LENGTH_LONG).asError().show()
        }
    }

    override fun onNewCustomFieldApproved(newField: Field) {
        if (entryEditFragment?.putCustomField(newField) != true)
            showAddCustomFieldError()
    }

    override fun onEditCustomFieldApproved(oldField: Field, newField: Field) {
        if (entryEditFragment?.replaceCustomField(oldField, newField) != true) {
            showAddCustomFieldError()
        }
    }

    override fun onDeleteCustomFieldApproved(oldField: Field) {
        entryEditFragment?.removeCustomField(oldField)
    }

    /**
     * Add a new attachment
     */
    private fun addNewAttachment() {
        mExternalFileHelper?.openDocument()
    }

    override fun onValidateUploadFileTooBig(attachmentToUploadUri: Uri?, fileName: String?) {
        if (attachmentToUploadUri != null && fileName != null) {
            buildNewAttachment(attachmentToUploadUri, fileName)
        }
    }

    override fun onValidateReplaceFile(attachmentToUploadUri: Uri?, attachment: Attachment?) {
        startUploadAttachment(attachmentToUploadUri, attachment)
    }

    private fun startUploadAttachment(attachmentToUploadUri: Uri?, attachment: Attachment?) {
        if (attachmentToUploadUri != null && attachment != null) {
            // When only one attachment is allowed
            if (!mAllowMultipleAttachments) {
                entryEditFragment?.clearAttachments()
            }
            // Start uploading in service
            mAttachmentFileBinderManager?.startUploadAttachment(attachmentToUploadUri, attachment)
        }
    }

    private fun buildNewAttachment(attachmentToUploadUri: Uri, fileName: String) {
        val compression = mDatabase?.compressionForNewEntry() ?: false
        mDatabase?.buildNewBinaryAttachment(compression)?.let { binaryAttachment ->
            val entryAttachment = Attachment(fileName, binaryAttachment)
            // Ask to replace the current attachment
            if ((mDatabase?.allowMultipleAttachments != true && entryEditFragment?.containsAttachment() == true) ||
                    entryEditFragment?.containsAttachment(EntryAttachmentState(entryAttachment, StreamDirection.UPLOAD)) == true) {
                ReplaceFileDialogFragment.build(attachmentToUploadUri, entryAttachment)
                        .show(supportFragmentManager, "replacementFileFragment")
            } else {
                startUploadAttachment(attachmentToUploadUri, entryAttachment)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        IconPickerActivity.onActivityResult(requestCode, resultCode, data) { icon ->
            entryEditFragment?.setIcon(icon)
        }

        mExternalFileHelper?.onOpenDocumentResult(requestCode, resultCode, data) { uri ->
            uri?.let { attachmentToUploadUri ->
                UriUtil.getFileData(this, attachmentToUploadUri)?.also { documentFile ->
                    documentFile.name?.let { fileName ->
                        if (documentFile.length() > MAX_WARNING_BINARY_FILE) {
                            FileTooBigDialogFragment.build(attachmentToUploadUri, fileName)
                                    .show(supportFragmentManager, "fileTooBigFragment")
                        } else {
                            buildNewAttachment(attachmentToUploadUri, fileName)
                        }
                    }
                }
            }
        }
    }

    /**
     * Set up OTP (HOTP or TOTP) and add it as extra field
     */
    private fun setupOTP() {
        // Retrieve the current otpElement if exists
        // and open the dialog to set up the OTP
        SetOTPDialogFragment.build(entryEditFragment?.getEntryInfo()?.otpModel)
                .show(supportFragmentManager, "addOTPDialog")
    }

    /**
     * Saves the new entry or update an existing entry in the database
     */
    private fun saveEntry() {
        mAttachmentFileBinderManager?.stopUploadAllAttachments()
        // Get the temp entry
        entryEditFragment?.getEntryInfo()?.let { newEntryInfo ->

            if (mIsNew) {
                // Create new one
                mDatabase?.createEntry()
            } else {
                // Create a clone
                Entry(mEntry!!)
            }?.let {
                var newEntry = it

                // Do not save entry in upload progression
                mTempAttachments.forEach { attachmentState ->
                    if (attachmentState.streamDirection == StreamDirection.UPLOAD) {
                        when (attachmentState.downloadState) {
                            AttachmentState.START,
                            AttachmentState.IN_PROGRESS,
                            AttachmentState.CANCELED,
                            AttachmentState.ERROR -> {
                                // Remove attachment not finished from info
                                newEntryInfo.attachments = newEntryInfo.attachments.toMutableList().apply {
                                    remove(attachmentState.attachment)
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }

                // Build info
                newEntry.setEntryInfo(mDatabase, newEntryInfo)

                // Encode entry properties for template
                val template = entryEditFragment?.getTemplate() ?: Template.STANDARD // TODO Move
                newEntry = mDatabase?.encodeEntryWithTemplateConfiguration(newEntry, template) ?: newEntry

                // Delete temp attachment if not used
                mTempAttachments.forEach { tempAttachmentState ->
                    val tempAttachment = tempAttachmentState.attachment
                    mDatabase?.attachmentPool?.let { binaryPool ->
                        if (!newEntry.getAttachments(binaryPool).contains(tempAttachment)) {
                            mDatabase?.removeAttachmentIfNotUsed(tempAttachment)
                        }
                    }
                }

                // Open a progress dialog and save entry
                if (mIsNew) {
                    mParent?.let { parent ->
                        mProgressDatabaseTaskProvider?.startDatabaseCreateEntry(
                                newEntry,
                                parent,
                                !mReadOnly && mAutoSaveEnable
                        )
                    }
                } else {
                    mEntry?.let { oldEntry ->
                        mProgressDatabaseTaskProvider?.startDatabaseUpdateEntry(
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
        menuInflater.inflate(R.menu.entry_edit, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        val allowCustomField = mDatabase?.allowEntryCustomFields() == true

        menu?.findItem(R.id.menu_add_field)?.apply {
            isEnabled = allowCustomField
            isVisible = isEnabled
        }

        menu?.findItem(R.id.menu_add_attachment)?.apply {
            // Attachment not compatible below KitKat
            isEnabled = !mIsTemplate
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            isVisible = isEnabled
        }

        menu?.findItem(R.id.menu_add_otp)?.apply {
            val allowOTP = mDatabase?.allowOTP == true
            // OTP not compatible below KitKat
            isEnabled = allowOTP
                    && !mIsTemplate
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            isVisible = isEnabled
        }

        entryEditActivityEducation?.let {
            Handler(Looper.getMainLooper()).post { performedNextEducation(it) }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun performedNextEducation(entryEditActivityEducation: EntryEditActivityEducation) {
        if (entryEditFragment?.generatePasswordEducationPerformed(entryEditActivityEducation) != true) {
            val addNewFieldView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_field)
            val addNewFieldEducationPerformed = mDatabase?.allowEntryCustomFields() == true
                    && addNewFieldView != null
                    && addNewFieldView.isVisible
                    && entryEditActivityEducation.checkAndPerformedEntryNewFieldEducation(
                    addNewFieldView,
                    {
                        addNewCustomField()
                    },
                    {
                        performedNextEducation(entryEditActivityEducation)
                    }
            )
            if (!addNewFieldEducationPerformed) {
                val attachmentView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_attachment)
                val addAttachmentEducationPerformed = attachmentView != null
                        && attachmentView.isVisible
                        && entryEditActivityEducation.checkAndPerformedAttachmentEducation(
                        attachmentView,
                        {
                            mExternalFileHelper?.openDocument()
                        },
                        {
                            performedNextEducation(entryEditActivityEducation)
                        }
                )
                if (!addAttachmentEducationPerformed) {
                    val setupOtpView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_otp)
                    setupOtpView != null
                            && setupOtpView.isVisible
                            && entryEditActivityEducation.checkAndPerformedSetUpOTPEducation(
                            setupOtpView,
                            {
                                setupOTP()
                            }
                    )
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_field -> {
                addNewCustomField()
                return true
            }
            R.id.menu_add_attachment -> {
                addNewAttachment()
                return true
            }
            R.id.menu_add_otp -> {
                setupOTP()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onOtpCreated(otpElement: OtpElement) {
        var titleOTP: String? = null
        var usernameOTP: String? = null
        // Build a temp entry to get title and username (by ref)
        entryEditFragment?.getEntryInfo()?.let { entryInfo ->
            val entryTemp = mDatabase?.createEntry()
            entryTemp?.setEntryInfo(mDatabase, entryInfo)
            mDatabase?.startManageEntry(entryTemp)
            titleOTP = entryTemp?.title
            usernameOTP = entryTemp?.username
            mDatabase?.stopManageEntry(mEntry)
        }
        // Update the otp field with otpauth:// url
        val otpField = OtpEntryFields.buildOtpField(otpElement, titleOTP, usernameOTP)
        mEntry?.putExtraField(Field(otpField.name, otpField.protectedValue))
        entryEditFragment?.apply {
            putCustomField(otpField)
        }
    }

    // Launch the date picker
    private fun selectDate(dateInstant: DateInstant) {
        val dateTime = DateTime(dateInstant.date)
        val defaultYear = dateTime.year
        val defaultMonth = dateTime.monthOfYear - 1
        val defaultDay = dateTime.dayOfMonth
        DatePickerFragment.getInstance(defaultYear, defaultMonth, defaultDay)
                .show(supportFragmentManager, "DatePickerFragment")
    }

    // Launch the time picker
    private fun selectTime(dateInstant: DateInstant) {
        val dateTime = DateTime(dateInstant.date)
        val defaultHour = dateTime.hourOfDay
        val defaultMinute = dateTime.minuteOfHour
        TimePickerFragment.getInstance(defaultHour, defaultMinute)
                .show(supportFragmentManager, "TimePickerFragment")
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        // To fix android 4.4 issue
        // https://stackoverflow.com/questions/12436073/datepicker-ondatechangedlistener-called-twice
        if (datePicker?.isShown == true) {
            entryEditFragment?.setDate(year, month, day)
        }
    }

    override fun onTimeSet(timePicker: TimePicker?, hours: Int, minutes: Int) {
        entryEditFragment?.setTime(hours, minutes)
    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putParcelableArrayList(TEMP_ATTACHMENTS, mTempAttachments)

        super.onSaveInstanceState(outState)
    }

    override fun acceptPassword(passwordField: Field) {
        entryEditFragment?.setPassword(passwordField)
        entryEditActivityEducation?.let {
            Handler(Looper.getMainLooper()).post { performedNextEducation(it) }
        }
    }

    override fun cancelPassword(passwordField: Field) {
        // Do nothing here
    }

    override fun onBackPressed() {
        onApprovedBackPressed {
            super@EntryEditActivity.onBackPressed()
        }
    }

    override fun onCancelSpecialMode() {
        onApprovedBackPressed {
            super@EntryEditActivity.onCancelSpecialMode()
            finish()
        }
    }

    private fun onApprovedBackPressed(approved: () -> Unit) {
        if (!backPressedAlreadyApproved) {
            AlertDialog.Builder(this)
                    .setMessage(R.string.discard_changes)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.discard) { _, _ ->
                        mAttachmentFileBinderManager?.stopUploadAllAttachments()
                        backPressedAlreadyApproved = true
                        approved.invoke()
                    }.create().show()
        } else {
            approved.invoke()
        }
    }

    private fun finishForEntryResult() {
        // Assign entry callback as a result
        try {
            mEntry?.let { entry ->
                val bundle = Bundle()
                val intentEntry = Intent()
                bundle.putParcelable(ADD_OR_UPDATE_ENTRY_KEY, entry)
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
        const val TEMP_ATTACHMENTS = "TEMP_ATTACHMENTS"

        // Keys for callback
        const val ADD_ENTRY_RESULT_CODE = 31
        const val UPDATE_ENTRY_RESULT_CODE = 32
        const val ADD_OR_UPDATE_ENTRY_REQUEST_CODE = 7129
        const val ADD_OR_UPDATE_ENTRY_KEY = "ADD_OR_UPDATE_ENTRY_KEY"

        const val ENTRY_EDIT_FRAGMENT_TAG = "ENTRY_EDIT_FRAGMENT_TAG"

        /**
         * Launch EntryEditActivity to update an existing entry
         *
         * @param activity from activity
         * @param entry Entry to update
         */
        fun launch(activity: Activity,
                   entry: Entry) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_ENTRY, entry.nodeId)
                activity.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry
         *
         * @param activity from activity
         * @param group Group who will contains new entry
         */
        fun launch(activity: Activity,
                   group: Group) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, group.nodeId)
                activity.startActivityForResult(intent, ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }

        fun launchForSave(context: Context,
                          entry: Entry,
                          searchInfo: SearchInfo) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                val intent = Intent(context, EntryEditActivity::class.java)
                intent.putExtra(KEY_ENTRY, entry.nodeId)
                EntrySelectionHelper.startActivityForSaveModeResult(context,
                        intent,
                        searchInfo)
            }
        }

        fun launchForSave(context: Context,
                          group: Group,
                          searchInfo: SearchInfo) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                val intent = Intent(context, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, group.nodeId)
                EntrySelectionHelper.startActivityForSaveModeResult(context,
                        intent,
                        searchInfo)
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry in keyboard selection
         */
        fun launchForKeyboardSelectionResult(context: Context,
                                             group: Group,
                                             searchInfo: SearchInfo? = null) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                val intent = Intent(context, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, group.nodeId)
                EntrySelectionHelper.startActivityForKeyboardSelectionModeResult(context,
                        intent,
                        searchInfo)
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry in autofill selection
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: Activity,
                                    autofillComponent: AutofillComponent,
                                    group: Group,
                                    searchInfo: SearchInfo? = null) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, group.nodeId)
                AutofillHelper.startActivityForAutofillResult(activity,
                        intent,
                        autofillComponent,
                        searchInfo)
            }
        }

        /**
         * Launch EntryEditActivity to register an updated entry (from autofill)
         */
        fun launchForRegistration(context: Context,
                                  entry: Entry,
                                  registerInfo: RegisterInfo? = null) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                val intent = Intent(context, EntryEditActivity::class.java)
                intent.putExtra(KEY_ENTRY, entry.nodeId)
                EntrySelectionHelper.startActivityForRegistrationModeResult(context,
                        intent,
                        registerInfo)
            }
        }

        /**
         * Launch EntryEditActivity to register a new entry (from autofill)
         */
        fun launchForRegistration(context: Context,
                                  group: Group,
                                  registerInfo: RegisterInfo? = null) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                val intent = Intent(context, EntryEditActivity::class.java)
                intent.putExtra(KEY_PARENT, group.nodeId)
                EntrySelectionHelper.startActivityForRegistrationModeResult(context,
                        intent,
                        registerInfo)
            }
        }
    }
}
