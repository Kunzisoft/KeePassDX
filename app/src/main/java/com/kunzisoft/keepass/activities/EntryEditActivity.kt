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
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.ColorPickerDialogFragment
import com.kunzisoft.keepass.activities.dialogs.EntryCustomFieldDialogFragment
import com.kunzisoft.keepass.activities.dialogs.FileTooBigDialogFragment
import com.kunzisoft.keepass.activities.dialogs.FileTooBigDialogFragment.Companion.MAX_WARNING_BINARY_FILE
import com.kunzisoft.keepass.activities.dialogs.ReplaceFileDialogFragment
import com.kunzisoft.keepass.activities.dialogs.SetOTPDialogFragment
import com.kunzisoft.keepass.activities.fragments.EntryEditFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.TemplatesSelectorAdapter
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.services.AttachmentFileNotificationService
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_CREATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.asError
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.ColorPickerViewModel
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel
import java.util.UUID

class EntryEditActivity : DatabaseLockActivity(),
        EntryCustomFieldDialogFragment.EntryCustomFieldListener,
        SetOTPDialogFragment.CreateOtpListener,
        FileTooBigDialogFragment.ActionChooseListener,
        ReplaceFileDialogFragment.ActionChooseListener {

    // Views
    private var footer: View? = null
    private var container: View? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var scrollView: NestedScrollView? = null
    private var templateSelectorSpinner: Spinner? = null
    private var entryEditAddToolBar: ToolbarAction? = null
    private var validateButton: View? = null
    private var lockView: View? = null
    private var loadingView: ProgressBar? = null

    private val mEntryEditViewModel: EntryEditViewModel by viewModels()
    private var mTemplate: Template? = null
    private var mIsTemplate: Boolean = false
    private var mEntryLoaded: Boolean = false
    private var mTemplatesSelectorAdapter: TemplatesSelectorAdapter? = null

    private val mColorPickerViewModel: ColorPickerViewModel by viewModels()

    private var mAllowCustomFields = false
    private var mAllowOTP = false

    // To manage attachments
    private var mExternalFileHelper: ExternalFileHelper? = null
    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    // Education
    private var mEntryEditActivityEducation = EntryEditActivityEducation(this)

    private var mIconSelectionActivityResultLauncher = IconPickerActivity.registerIconSelectionForResult(this) { icon ->
        mEntryEditViewModel.selectIcon(icon)
    }

    private var mPasswordField: Field? = null
    private var mKeyGeneratorResultLauncher = KeyGeneratorActivity.registerForGeneratedKeyResult(this) { keyGenerated ->
        keyGenerated?.let {
            mPasswordField?.let {
                it.protectedValue.stringValue = keyGenerated
                mEntryEditViewModel.selectPassword(it)
            }
        }
        mPasswordField = null
        Handler(Looper.getMainLooper()).post {
            performedNextEducation()
        }
    }

    // To ask data lost only one time
    private var backPressedAlreadyApproved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_edit)

        // Bottom Bar
        entryEditAddToolBar = findViewById(R.id.entry_edit_bottom_bar)
        footer = findViewById(R.id.activity_entry_edit_footer)
        container = findViewById(R.id.activity_entry_edit_container)
        coordinatorLayout = findViewById(R.id.entry_edit_coordinator_layout)
        scrollView = findViewById(R.id.entry_edit_scroll)
        scrollView?.scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        templateSelectorSpinner = findViewById(R.id.entry_edit_template_selector)
        lockView = findViewById(R.id.lock_button)
        validateButton = findViewById(R.id.entry_edit_validate)
        loadingView = findViewById(R.id.loading)

        setSupportActionBar(entryEditAddToolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // To apply fit window with transparency
        setTransparentNavigationBar(applyToStatusBar = true) {
            container?.applyWindowInsets(WindowInsetPosition.TOP_BOTTOM_IME)
            footer?.applyWindowInsets(WindowInsetPosition.BOTTOM_IME)
        }

        stopService(Intent(this, ClipboardEntryNotificationService::class.java))
        stopService(Intent(this, KeyboardEntryNotificationService::class.java))

        // Entry is retrieve, it's an entry to update
        var entryId: NodeId<UUID>? = null
        intent.getParcelableExtraCompat<NodeId<UUID>>(KEY_ENTRY)?.let { entryToUpdate ->
            intent.removeExtra(KEY_ENTRY)
            entryId = entryToUpdate
        }

        // Parent is retrieve, it's a new entry to create
        var parentId: NodeId<*>? = null
        intent.getParcelableExtraCompat<NodeId<*>>(KEY_PARENT)?.let { parent ->
            intent.removeExtra(KEY_PARENT)
            parentId = parent
        }

        mEntryEditViewModel.loadTemplateEntry(
            mDatabase,
            entryId,
            parentId,
            EntrySelectionHelper.retrieveRegisterInfoFromIntent(intent),
            EntrySelectionHelper.retrieveSearchInfoFromIntent(intent)
        )

        // To retrieve attachment
        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildOpenDocument { uri ->
            uri?.let { attachmentToUploadUri ->
                attachmentToUploadUri.getDocumentFile(this)?.also { documentFile ->
                    documentFile.name?.let { fileName ->
                        if (documentFile.length() > MAX_WARNING_BINARY_FILE) {
                            FileTooBigDialogFragment.build(attachmentToUploadUri, fileName)
                                .show(supportFragmentManager, "fileTooBigFragment")
                        } else {
                            mEntryEditViewModel.buildNewAttachment(attachmentToUploadUri, fileName)
                        }
                    }
                }
            }
        }

        mAttachmentFileBinderManager = AttachmentFileBinderManager(this)

        // Lock button
        lockView?.setOnClickListener { lockAndExit() }
        // Save button
        validateButton?.setOnClickListener { validateEntry() }

        mEntryEditViewModel.onTemplateChanged.observe(this) { template ->
            this.mTemplate = template
        }

        mEntryEditViewModel.templatesEntry.observe(this) { templatesEntry ->
            if (templatesEntry != null) {
                // Change template dynamically
                this.mIsTemplate = templatesEntry.isTemplate
                templatesEntry.templates.let { templates ->
                    templateSelectorSpinner?.apply {
                        // Build template selector
                        if (templates.isNotEmpty()) {
                            mTemplatesSelectorAdapter = TemplatesSelectorAdapter(
                                this@EntryEditActivity,
                                templates
                            ).apply {
                                iconDrawableFactory = mDatabase?.iconDrawableFactory
                            }
                            adapter = mTemplatesSelectorAdapter
                            val selectedTemplate = if (mTemplate != null)
                                mTemplate
                            else
                                templatesEntry.defaultTemplate
                            setSelection(templates.indexOf(selectedTemplate))
                            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    mEntryEditViewModel.changeTemplate(templates[position])
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                            }
                        } else {
                            visibility = View.GONE
                        }
                    }
                }

                loadingView?.hideByFading()
                mEntryLoaded = true
            } else {
                finish()
            }
            invalidateOptionsMenu()
        }

        // View model listeners
        mEntryEditViewModel.requestIconSelection.observe(this) { iconImage ->
            IconPickerActivity.launch(this@EntryEditActivity, iconImage, mIconSelectionActivityResultLauncher)
        }

        mEntryEditViewModel.requestColorSelection.observe(this) { color ->
            ColorPickerDialogFragment.newInstance(color)
                .show(supportFragmentManager, "ColorPickerFragment")
        }

        mColorPickerViewModel.colorPicked.observe(this) { color ->
            mEntryEditViewModel.selectColor(color)
        }

        mEntryEditViewModel.requestDateTimeSelection.observe(this) { dateInstant ->
            if (dateInstant.type == DateInstant.Type.TIME) {
                // Launch the time picker
                MaterialTimePicker.Builder().build().apply {
                    addOnPositiveButtonClickListener {
                        mEntryEditViewModel.selectTime(this.hour, this.minute)
                    }
                    show(supportFragmentManager, "TimePickerFragment")
                }
            } else {
                // Launch the date picker
                MaterialDatePicker.Builder.datePicker().build().apply {
                    addOnPositiveButtonClickListener {
                        mEntryEditViewModel.selectDate(it)
                    }
                    show(supportFragmentManager, "DatePickerFragment")
                }
            }
        }

        mEntryEditViewModel.requestPasswordSelection.observe(this) { passwordField ->
            mPasswordField = passwordField
            KeyGeneratorActivity.launch(this, mKeyGeneratorResultLauncher)
        }

        mEntryEditViewModel.requestCustomFieldEdition.observe(this) { field ->
            editCustomField(field)
        }

        mEntryEditViewModel.onCustomFieldError.observe(this) {
            coordinatorLayout?.let {
                Snackbar.make(it, R.string.error_field_name_already_exists, Snackbar.LENGTH_LONG)
                        .asError()
                        .show()
            }
        }

        mEntryEditViewModel.onStartUploadAttachment.observe(this) {
            // Start uploading in service
            mAttachmentFileBinderManager?.startUploadAttachment(it.attachmentToUploadUri, it.attachment)
        }

        mEntryEditViewModel.onAttachmentAction.observe(this) { attachmentState ->
            when (attachmentState?.downloadState) {
                AttachmentState.ERROR -> {
                    coordinatorLayout?.let {
                        Snackbar.make(it, R.string.error_file_not_create, Snackbar.LENGTH_LONG).asError().show()
                    }
                }
                else -> {}
            }
        }

        mEntryEditViewModel.onBinaryPreviewLoaded.observe(this) {
            // Scroll to the attachment position
            when (it.entryAttachmentState.downloadState) {
                AttachmentState.START,
                AttachmentState.COMPLETE -> {
                    scrollView?.smoothScrollTo(0, it.viewPosition.toInt())
                }
                else -> {}
            }
        }

        mEntryEditViewModel.attachmentDeleted.observe(this) {
            mAttachmentFileBinderManager?.removeBinaryAttachment(it)
        }

        // Build new entry from the entry info retrieved
        mEntryEditViewModel.onEntrySaved.observe(this) { entrySave ->
            // Open a progress dialog and save entry
            entrySave.parent?.let { parent ->
                createEntry(entrySave.newEntry, parent)
            } ?: run {
                updateEntry(entrySave.oldEntry, entrySave.newEntry)
            }

            // Don't wait for saving if it's to provide autofill
            mDatabase?.let { database ->
                EntrySelectionHelper.doSpecialAction(intent,
                    {},
                    {},
                    {},
                    {
                        entryValidatedForKeyboardSelection(database, entrySave.newEntry)
                    },
                    { _, _ ->
                        entryValidatedForAutofillSelection(database, entrySave.newEntry)
                    },
                    {
                        entryValidatedForAutofillRegistration(entrySave.newEntry)
                    }
                )
            }
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        mAllowCustomFields = database?.allowEntryCustomFields() == true
        mAllowOTP = database?.allowOTP == true
        mEntryEditViewModel.loadDatabase(database)
        mTemplatesSelectorAdapter?.apply {
            iconDrawableFactory = mDatabase?.iconDrawableFactory
            notifyDataSetChanged()
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_CREATE_ENTRY_TASK,
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                try {
                    if (result.isSuccess) {
                        var newNodes: List<Node> = ArrayList()
                        result.data?.getBundle(DatabaseTaskNotificationService.NEW_NODES_KEY)?.let { newNodesBundle ->
                            newNodes = DatabaseTaskNotificationService.getListNodesFromBundle(database, newNodesBundle)
                        }
                        if (newNodes.size == 1) {
                            (newNodes[0] as? Entry?)?.let { entry ->
                                EntrySelectionHelper.doSpecialAction(intent,
                                    {
                                        // Finish naturally
                                        finishForEntryResult(entry)
                                    },
                                    {
                                        // Nothing when search retrieved
                                    },
                                    {
                                        entryValidatedForSave(entry)
                                    },
                                    {
                                        entryValidatedForKeyboardSelection(database, entry)
                                    },
                                    { _, _ ->
                                        entryValidatedForAutofillSelection(database, entry)
                                    },
                                    {
                                        entryValidatedForAutofillRegistration(entry)
                                    }
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve entry after database action", e)
                }
            }
        }
        coordinatorLayout?.showActionErrorIfNeeded(result)
    }

    private fun entryValidatedForSave(entry: Entry) {
        onValidateSpecialMode()
        finishForEntryResult(entry)
    }

    private fun entryValidatedForKeyboardSelection(database: ContextualDatabase, entry: Entry) {
        // Populate Magikeyboard with entry
        MagikeyboardService.populateKeyboardAndMoveAppToBackground(
            this,
            entry.getEntryInfo(database)
        )
        onValidateSpecialMode()
        // Don't keep activity history for entry edition
        finishForEntryResult(entry)
    }

    private fun entryValidatedForAutofillSelection(database: ContextualDatabase, entry: Entry) {
        // Build Autofill response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.buildResponseAndSetResult(this@EntryEditActivity,
                    database,
                    entry.getEntryInfo(database))
        }
        onValidateSpecialMode()
    }

    private fun entryValidatedForAutofillRegistration(entry: Entry) {
        onValidateSpecialMode()
        finishForEntryResult(entry)
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

        mAttachmentFileBinderManager?.apply {
            registerProgressTask()
            onActionTaskListener = object : AttachmentFileNotificationService.ActionTaskListener {
                override fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState) {
                    mEntryEditViewModel.onAttachmentAction(entryAttachmentState)
                }
            }
        }

        // Keep the screen on
        if (PreferencesUtil.isKeepScreenOnEnabled(this)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
        EntryCustomFieldDialogFragment.getInstance().show(supportFragmentManager, "customFieldDialog")
    }

    private fun editCustomField(field: Field) {
        EntryCustomFieldDialogFragment.getInstance(field).show(supportFragmentManager, "customFieldDialog")
    }

    override fun onNewCustomFieldApproved(newField: Field) {
        mEntryEditViewModel.addCustomField(newField)
    }

    override fun onEditCustomFieldApproved(oldField: Field, newField: Field) {
        mEntryEditViewModel.editCustomField(oldField, newField)
    }

    override fun onDeleteCustomFieldApproved(oldField: Field) {
        mEntryEditViewModel.removeCustomField(oldField)
    }

    /**
     * Add a new attachment
     */
    private fun addNewAttachment() {
        mExternalFileHelper?.openDocument()
    }

    override fun onValidateUploadFileTooBig(attachmentToUploadUri: Uri?, fileName: String?) {
        if (attachmentToUploadUri != null && fileName != null) {
            mEntryEditViewModel.buildNewAttachment(attachmentToUploadUri, fileName)
        }
    }

    override fun onValidateReplaceFile(attachmentToUploadUri: Uri?, attachment: Attachment?) {
        if (attachmentToUploadUri != null && attachment != null) {
            mEntryEditViewModel.startUploadAttachment(attachmentToUploadUri, attachment)
        }
    }

    /**
     * Set up OTP (HOTP or TOTP) and add it as extra field
     */
    private fun setupOtp() {
        mEntryEditViewModel.setupOtp()
    }

    override fun onOtpCreated(otpElement: OtpElement) {
        mEntryEditViewModel.createOtp(otpElement)
    }

    /**
     * Validate the new entry or update an existing entry in the database
     */
    private fun validateEntry() {
        mAttachmentFileBinderManager?.stopUploadAllAttachments()
        mEntryEditViewModel.requestEntryInfoUpdate(mDatabase)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (mEntryLoaded) {
            menuInflater.inflate(R.menu.entry_edit, menu)
            Handler(Looper.getMainLooper()).post {
                performedNextEducation()
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_add_field)?.apply {
            isEnabled = mAllowCustomFields
            isVisible = isEnabled
        }
        menu?.findItem(R.id.menu_add_attachment)?.apply {
            // Attachment not compatible below KitKat
            isEnabled = !mIsTemplate
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            isVisible = isEnabled
        }
        menu?.findItem(R.id.menu_add_otp)?.apply {
            // OTP not compatible below KitKat
            isEnabled = mAllowOTP
                    && !mIsTemplate
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            isVisible = isEnabled
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun performedNextEducation() {

        val entryEditFragment = supportFragmentManager.findFragmentById(R.id.entry_edit_content)
                as? EntryEditFragment?
        val generatePasswordView = entryEditFragment?.getActionImageView()
        val generatePasswordEductionPerformed = generatePasswordView != null
                && mEntryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
            generatePasswordView,
            {
                entryEditFragment.launchGeneratePasswordEductionAction()
            },
            {
                performedNextEducation()
            }
        )

        if (!generatePasswordEductionPerformed) {
            val addNewFieldView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_field)
            val addNewFieldEducationPerformed = mAllowCustomFields
                    && addNewFieldView != null
                    && addNewFieldView.isVisible
                    && mEntryEditActivityEducation.checkAndPerformedEntryNewFieldEducation(
                    addNewFieldView,
                    {
                        addNewCustomField()
                    },
                    {
                        performedNextEducation()
                    }
            )
            if (!addNewFieldEducationPerformed) {
                val attachmentView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_attachment)
                val addAttachmentEducationPerformed = attachmentView != null
                        && attachmentView.isVisible
                        && mEntryEditActivityEducation.checkAndPerformedAttachmentEducation(
                        attachmentView,
                        {
                            addNewAttachment()
                        },
                        {
                            performedNextEducation()
                        }
                )
                if (!addAttachmentEducationPerformed) {
                    val setupOtpView: View? = entryEditAddToolBar?.findViewById(R.id.menu_add_otp)
                    val validateEntryEducationPerformed = setupOtpView != null
                            && setupOtpView.isVisible
                            && mEntryEditActivityEducation.checkAndPerformedSetUpOTPEducation(
                            setupOtpView,
                            {
                                setupOtp()
                            },
                            {
                                performedNextEducation()
                            }
                    )
                    if (!validateEntryEducationPerformed) {
                        val entryValidateView = validateButton
                        mAllowCustomFields
                                && entryValidateView != null
                                && entryValidateView.isVisible
                                && mEntryEditActivityEducation.checkAndPerformedValidateEntryEducation(
                                entryValidateView,
                                {
                                    validateEntry()
                                }
                        )
                    }
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
                setupOtp()
                return true
            }
            android.R.id.home -> {
                onDatabaseBackPressed()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDatabaseBackPressed() {
        onApprovedBackPressed {
            super@EntryEditActivity.onDatabaseBackPressed()
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

    private fun finishForEntryResult(entry: Entry) {
        // Assign entry callback as a result
        try {
            val bundle = Bundle()
            val intentEntry = Intent()
            bundle.putParcelable(ADD_OR_UPDATE_ENTRY_KEY, entry.nodeId)
            intentEntry.putExtras(bundle)
            setResult(Activity.RESULT_OK, intentEntry)
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
        const val ADD_OR_UPDATE_ENTRY_KEY = "ADD_OR_UPDATE_ENTRY_KEY"

        fun registerForEntryResult(fragment: Fragment,
                                   entryAddedOrUpdatedListener: (NodeId<UUID>?) -> Unit): ActivityResultLauncher<Intent> {
            return fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    entryAddedOrUpdatedListener.invoke(
                        result.data?.getParcelableExtraCompat(ADD_OR_UPDATE_ENTRY_KEY)
                    )
                } else {
                    entryAddedOrUpdatedListener.invoke(null)
                }
            }
        }

        fun registerForEntryResult(activity: FragmentActivity,
                                   entryAddedOrUpdatedListener: (NodeId<UUID>?) -> Unit): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    entryAddedOrUpdatedListener.invoke(
                        result.data?.getParcelableExtraCompat(ADD_OR_UPDATE_ENTRY_KEY)
                    )
                } else {
                    entryAddedOrUpdatedListener.invoke(null)
                }
            }
        }

        /**
         * Launch EntryEditActivity to update an existing entry by his [entryId]
         */
        fun launchToUpdate(activity: Activity,
                           database: ContextualDatabase,
                           entryId: NodeId<UUID>,
                           activityResultLauncher: ActivityResultLauncher<Intent>) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryEditActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    activityResultLauncher.launch(intent)
                }
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry in an existent group
         */
        fun launchToCreate(activity: Activity,
                           database: ContextualDatabase,
                           groupId: NodeId<*>,
                           activityResultLauncher: ActivityResultLauncher<Intent>) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryEditActivity::class.java)
                    intent.putExtra(KEY_PARENT, groupId)
                    activityResultLauncher.launch(intent)
                }
            }
        }

        fun launchToUpdateForSave(context: Context,
                                  database: ContextualDatabase,
                                  entryId: NodeId<UUID>,
                                  searchInfo: SearchInfo) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                    val intent = Intent(context, EntryEditActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    EntrySelectionHelper.startActivityForSaveModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        fun launchToCreateForSave(context: Context,
                                  database: ContextualDatabase,
                                  groupId: NodeId<*>,
                                  searchInfo: SearchInfo) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                    val intent = Intent(context, EntryEditActivity::class.java)
                    intent.putExtra(KEY_PARENT, groupId)
                    EntrySelectionHelper.startActivityForSaveModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry in keyboard selection
         */
        fun launchForKeyboardSelectionResult(context: Context,
                                             database: ContextualDatabase,
                                             groupId: NodeId<*>,
                                             searchInfo: SearchInfo? = null) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                    val intent = Intent(context, EntryEditActivity::class.java)
                    intent.putExtra(KEY_PARENT, groupId)
                    EntrySelectionHelper.startActivityForKeyboardSelectionModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /**
         * Launch EntryEditActivity to add a new entry in autofill selection
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: AppCompatActivity,
                                    database: ContextualDatabase,
                                    activityResultLauncher: ActivityResultLauncher<Intent>?,
                                    autofillComponent: AutofillComponent,
                                    groupId: NodeId<*>,
                                    searchInfo: SearchInfo? = null) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryEditActivity::class.java)
                    intent.putExtra(KEY_PARENT, groupId)
                    AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
                        activityResultLauncher,
                        autofillComponent,
                        searchInfo
                    )
                }
            }
        }

        /**
         * Launch EntryEditActivity to register an updated entry (from autofill)
         */
        fun launchToUpdateForRegistration(context: Context,
                                          database: ContextualDatabase,
                                          entryId: NodeId<UUID>,
                                          registerInfo: RegisterInfo? = null) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                    val intent = Intent(context, EntryEditActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    EntrySelectionHelper.startActivityForRegistrationModeResult(
                        context,
                        intent,
                        registerInfo
                    )
                }
            }
        }

        /**
         * Launch EntryEditActivity to register a new entry (from autofill)
         */
        fun launchToCreateForRegistration(context: Context,
                                          database: ContextualDatabase,
                                          groupId: NodeId<*>,
                                          registerInfo: RegisterInfo? = null) {
            if (database.loaded && !database.isReadOnly) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                    val intent = Intent(context, EntryEditActivity::class.java)
                    intent.putExtra(KEY_PARENT, groupId)
                    EntrySelectionHelper.startActivityForRegistrationModeResult(
                        context,
                        intent,
                        registerInfo
                    )
                }
            }
        }
    }
}
