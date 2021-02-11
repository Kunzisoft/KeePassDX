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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ReadOnlyHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.education.EntryActivityEducation
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.services.AttachmentFileNotificationService
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_ENTRY_HISTORY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RELOAD_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RESTORE_ENTRY_HISTORY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.*
import com.kunzisoft.keepass.view.EntryContentsView
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import java.util.*
import kotlin.collections.HashMap

class EntryActivity : LockingActivity() {

    private var coordinatorLayout: CoordinatorLayout? = null
    private var collapsingToolbarLayout: CollapsingToolbarLayout? = null
    private var titleIconView: ImageView? = null
    private var historyView: View? = null
    private var entryContentsView: EntryContentsView? = null
    private var entryProgress: ProgressBar? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null

    private var mDatabase: Database? = null

    private var mEntry: Entry? = null

    private var mIsHistory: Boolean = false
    private var mEntryLastVersion: Entry? = null
    private var mEntryHistoryPosition: Int = -1

    private var mShowPassword: Boolean = false

    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    private var mAttachmentsToDownload: HashMap<Int, Attachment> = HashMap()

    private var clipboardHelper: ClipboardHelper? = null
    private var mFirstLaunchOfActivity: Boolean = false

    private var iconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_entry)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mDatabase = Database.getInstance()
        mReadOnly = mDatabase!!.isReadOnly || mReadOnly

        mShowPassword = !PreferencesUtil.isPasswordMask(this)

        // Retrieve the textColor to tint the icon
        val taIconColor = theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
        iconColor = taIconColor.getColor(0, Color.BLACK)
        taIconColor.recycle()

        // Refresh Menu contents in case onCreateMenuOptions was called before mEntry was set
        invalidateOptionsMenu()

        // Get views
        coordinatorLayout = findViewById(R.id.toolbar_coordinator)
        collapsingToolbarLayout = findViewById(R.id.toolbar_layout)
        titleIconView = findViewById(R.id.entry_icon)
        historyView = findViewById(R.id.history_container)
        entryContentsView = findViewById(R.id.entry_contents)
        entryContentsView?.applyFontVisibilityToFields(PreferencesUtil.fieldFontIsInVisibility(this))
        entryContentsView?.setAttachmentCipherKey(mDatabase?.loadedCipherKey)
        entryProgress = findViewById(R.id.entry_progress)
        lockView = findViewById(R.id.lock_button)

        lockView?.setOnClickListener {
            lockAndExit()
        }

        // Focus view to reinitialize timeout
        coordinatorLayout?.resetAppTimeoutWhenViewFocusedOrChanged(this)

        // Init the clipboard helper
        clipboardHelper = ClipboardHelper(this)
        mFirstLaunchOfActivity = savedInstanceState?.getBoolean(KEY_FIRST_LAUNCH_ACTIVITY) ?: true

        // Init attachment service binder manager
        mAttachmentFileBinderManager = AttachmentFileBinderManager(this)

        mProgressDatabaseTaskProvider?.onActionFinish = { actionTask, result ->
            when (actionTask) {
                ACTION_DATABASE_RESTORE_ENTRY_HISTORY,
                ACTION_DATABASE_DELETE_ENTRY_HISTORY -> {
                    // Close the current activity after an history action
                    if (result.isSuccess)
                        finish()
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

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Get Entry from UUID
        try {
            val keyEntry: NodeId<UUID>? = intent.getParcelableExtra(KEY_ENTRY)
            if (keyEntry != null) {
                mEntry = mDatabase?.getEntryById(keyEntry)
                mEntryLastVersion = mEntry
            }
        } catch (e: ClassCastException) {
            Log.e(TAG, "Unable to retrieve the entry key")
        }

        val historyPosition = intent.getIntExtra(KEY_ENTRY_HISTORY_POSITION, mEntryHistoryPosition)
        mEntryHistoryPosition = historyPosition
        if (historyPosition >= 0) {
            mIsHistory = true
            mEntry = mEntry?.getHistory()?.get(historyPosition)
        }

        if (mEntry == null) {
            Toast.makeText(this, R.string.entry_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Update last access time.
        mEntry?.touch(modified = false, touchParents = false)

        mEntry?.let { entry ->
            // Fill data in resume to update from EntryEditActivity
            fillEntryDataInContentsView(entry)
            // Refresh Menu
            invalidateOptionsMenu()

            val entryInfo = entry.getEntryInfo(mDatabase)
            // Manage entry copy to start notification if allowed
            if (mFirstLaunchOfActivity) {
                // Manage entry to launch copying notification if allowed
                ClipboardEntryNotificationService.launchNotificationIfAllowed(this, entryInfo)
                // Manage entry to populate Magikeyboard and launch keyboard notification if allowed
                if (PreferencesUtil.isKeyboardEntrySelectionEnable(this)) {
                    MagikIME.addEntryAndLaunchNotificationIfAllowed(this, entryInfo)
                }
            }
        }

        mAttachmentFileBinderManager?.apply {
            registerProgressTask()
            onActionTaskListener = object : AttachmentFileNotificationService.ActionTaskListener {
                override fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState) {
                    if (entryAttachmentState.streamDirection != StreamDirection.UPLOAD) {
                        entryContentsView?.putAttachment(entryAttachmentState)
                    }
                }
            }
        }

        mFirstLaunchOfActivity = false
    }

    override fun onPause() {
        mAttachmentFileBinderManager?.unregisterProgressTask()

        super.onPause()
    }

    private fun fillEntryDataInContentsView(entry: Entry) {

        val entryInfo = entry.getEntryInfo(mDatabase)

        // Assign title icon
        titleIconView?.assignDatabaseIcon(mDatabase!!.drawFactory, entryInfo.icon, iconColor)

        // Assign title text
        val entryTitle = entryInfo.title
        collapsingToolbarLayout?.title = entryTitle
        toolbar?.title = entryTitle

        // Assign basic fields
        entryContentsView?.assignUserName(entryInfo.username) {
            clipboardHelper?.timeoutCopyToClipboard(entryInfo.username,
                    getString(R.string.copy_field,
                            getString(R.string.entry_user_name)))
        }

        val isFirstTimeAskAllowCopyPasswordAndProtectedFields =
                PreferencesUtil.isFirstTimeAskAllowCopyPasswordAndProtectedFields(this)
        val allowCopyPasswordAndProtectedFields =
                PreferencesUtil.allowCopyPasswordAndProtectedFields(this)

        val showWarningClipboardDialogOnClickListener = View.OnClickListener {
            AlertDialog.Builder(this@EntryActivity)
                    .setMessage(getString(R.string.allow_copy_password_warning) +
                            "\n\n" +
                            getString(R.string.clipboard_warning))
                    .create().apply {
                        setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.enable)) { dialog, _ ->
                            PreferencesUtil.setAllowCopyPasswordAndProtectedFields(this@EntryActivity, true)
                            dialog.dismiss()
                            fillEntryDataInContentsView(entry)
                        }
                        setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable)) { dialog, _ ->
                            PreferencesUtil.setAllowCopyPasswordAndProtectedFields(this@EntryActivity, false)
                            dialog.dismiss()
                            fillEntryDataInContentsView(entry)
                        }
                        show()
                    }
        }

        val onPasswordCopyClickListener: View.OnClickListener? = if (allowCopyPasswordAndProtectedFields) {
            View.OnClickListener {
                clipboardHelper?.timeoutCopyToClipboard(entryInfo.password,
                        getString(R.string.copy_field,
                                getString(R.string.entry_password)))
            }
        } else {
            // If dialog not already shown
            if (isFirstTimeAskAllowCopyPasswordAndProtectedFields) {
                showWarningClipboardDialogOnClickListener
            } else {
                null
            }
        }
        entryContentsView?.assignPassword(entryInfo.password,
                allowCopyPasswordAndProtectedFields,
                onPasswordCopyClickListener)

        //Assign OTP field
        entry.getOtpElement()?.let { otpElement ->
            entryContentsView?.assignOtp(otpElement, entryProgress) {
                clipboardHelper?.timeoutCopyToClipboard(
                        otpElement.token,
                        getString(R.string.copy_field, getString(R.string.entry_otp))
                )
            }
        }

        entryContentsView?.assignURL(entryInfo.url)
        entryContentsView?.assignNotes(entryInfo.notes)

        // Assign custom fields
        if (mDatabase?.allowEntryCustomFields() == true) {
            entryContentsView?.clearExtraFields()
            entryInfo.customFields.forEach { field ->
                val label = field.name
                // OTP field is already managed in dedicated view
                if (label != OtpEntryFields.OTP_TOKEN_FIELD) {
                    val value = field.protectedValue
                    val allowCopyProtectedField = !value.isProtected || allowCopyPasswordAndProtectedFields
                    if (allowCopyProtectedField) {
                        entryContentsView?.addExtraField(label, value, allowCopyProtectedField) {
                            clipboardHelper?.timeoutCopyToClipboard(
                                    value.toString(),
                                    getString(R.string.copy_field, label)
                            )
                        }
                    } else {
                        // If dialog not already shown
                        if (isFirstTimeAskAllowCopyPasswordAndProtectedFields) {
                            entryContentsView?.addExtraField(label, value, allowCopyProtectedField, showWarningClipboardDialogOnClickListener)
                        } else {
                            entryContentsView?.addExtraField(label, value, allowCopyProtectedField, null)
                        }
                    }
                }
            }
        }
        entryContentsView?.setHiddenProtectedValue(!mShowPassword)

        // Manage attachments
        entryContentsView?.assignAttachments(entryInfo.attachments.toSet(), StreamDirection.DOWNLOAD) { attachmentItem ->
            createDocument(this, attachmentItem.name)?.let { requestCode ->
                mAttachmentsToDownload[requestCode] = attachmentItem
            }
        }

        // Assign dates
        entryContentsView?.assignCreationDate(entryInfo.creationTime)
        entryContentsView?.assignModificationDate(entryInfo.lastModificationTime)
        entryContentsView?.setExpires(entryInfo.expires, entryInfo.expiryTime)

        // Manage history
        historyView?.visibility = if (mIsHistory) View.VISIBLE else View.GONE
        if (mIsHistory) {
            val taColorAccent = theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
            collapsingToolbarLayout?.contentScrim = ColorDrawable(taColorAccent.getColor(0, Color.BLACK))
            taColorAccent.recycle()
        }
        entryContentsView?.assignHistory(entry.getHistory()) { historyItem, position ->
            launch(this, historyItem, mReadOnly, position)
        }

        // Assign special data
        entryContentsView?.assignUUID(entry.nodeId.id)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE ->
                // Not directly get the entry from intent data but from database
                mEntry?.let {
                    fillEntryDataInContentsView(it)
                }
        }

        onCreateDocumentResult(requestCode, resultCode, data) { createdFileUri ->
            if (createdFileUri != null) {
                mAttachmentsToDownload[requestCode]?.let { attachmentToDownload ->
                    mAttachmentFileBinderManager
                            ?.startDownloadAttachment(createdFileUri, attachmentToDownload)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val inflater = menuInflater
        MenuUtil.contributionMenuInflater(inflater, menu)
        inflater.inflate(R.menu.entry, menu)
        inflater.inflate(R.menu.database, menu)
        if (mIsHistory && !mReadOnly) {
            inflater.inflate(R.menu.entry_history, menu)
        }
        if (mIsHistory || mReadOnly) {
            menu.findItem(R.id.menu_save_database)?.isVisible = false
            menu.findItem(R.id.menu_edit)?.isVisible = false
        }
        if (mSpecialMode != SpecialMode.DEFAULT) {
            menu.findItem(R.id.menu_reload_database)?.isVisible = false
        }

        val gotoUrl = menu.findItem(R.id.menu_goto_url)
        gotoUrl?.apply {
            // In API >= 11 onCreateOptionsMenu may be called before onCreate completes
            // so mEntry may not be set
            if (mEntry == null) {
                isVisible = false
            } else {
                if (mEntry?.url?.isEmpty() != false) {
                    // disable button if url is not available
                    isVisible = false
                }
            }
        }

        // Show education views
        Handler(Looper.getMainLooper()).post { performedNextEducation(EntryActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(entryActivityEducation: EntryActivityEducation,
                                       menu: Menu) {
        val entryFieldCopyView = entryContentsView?.firstEntryFieldCopyView()
        val entryCopyEducationPerformed = entryFieldCopyView != null
                && entryActivityEducation.checkAndPerformedEntryCopyEducation(
                        entryFieldCopyView,
                        {
                            val appNameString = getString(R.string.app_name)
                            clipboardHelper?.timeoutCopyToClipboard(appNameString,
                                    getString(R.string.copy_field, appNameString))
                        },
                        {
                            performedNextEducation(entryActivityEducation, menu)
                        })

        if (!entryCopyEducationPerformed) {
            val menuEditView = toolbar?.findViewById<View>(R.id.menu_edit)
            // entryEditEducationPerformed
            menuEditView != null && entryActivityEducation.checkAndPerformedEntryEditEducation(
                    menuEditView,
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_edit))
                    },
                    {
                        performedNextEducation(entryActivityEducation, menu)
                    }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_contribute -> {
                MenuUtil.onContributionItemSelected(this)
                return true
            }
            R.id.menu_edit -> {
                mEntry?.let {
                    EntryEditActivity.launch(this@EntryActivity, it)
                }
                return true
            }
            R.id.menu_goto_url -> {
                var url: String = mEntry?.url ?: ""

                // Default http:// if no protocol specified
                if (!url.contains("://")) {
                    url = "http://$url"
                }

                UriUtil.gotoUrl(this, url)
                return true
            }
            R.id.menu_restore_entry_history -> {
                mEntryLastVersion?.let { mainEntry ->
                    mProgressDatabaseTaskProvider?.startDatabaseRestoreEntryHistory(
                            mainEntry,
                            mEntryHistoryPosition,
                            !mReadOnly && mAutoSaveEnable)
                }
            }
            R.id.menu_delete_entry_history -> {
                mEntryLastVersion?.let { mainEntry ->
                    mProgressDatabaseTaskProvider?.startDatabaseDeleteEntryHistory(
                            mainEntry,
                            mEntryHistoryPosition,
                            !mReadOnly && mAutoSaveEnable)
                }
            }
            R.id.menu_save_database -> {
                mProgressDatabaseTaskProvider?.startDatabaseSave(!mReadOnly)
            }
            R.id.menu_reload_database -> {
                mProgressDatabaseTaskProvider?.startDatabaseReload(false)
            }
            android.R.id.home -> finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(KEY_FIRST_LAUNCH_ACTIVITY, mFirstLaunchOfActivity)
    }

    override fun finish() {
        // Transit data in previous Activity after an update
        Intent().apply {
            putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mEntry)
            setResult(EntryEditActivity.UPDATE_ENTRY_RESULT_CODE, this)
        }
        super.finish()
    }

    companion object {
        private val TAG = EntryActivity::class.java.name

        private const val KEY_FIRST_LAUNCH_ACTIVITY = "KEY_FIRST_LAUNCH_ACTIVITY"

        const val KEY_ENTRY = "KEY_ENTRY"
        const val KEY_ENTRY_HISTORY_POSITION = "KEY_ENTRY_HISTORY_POSITION"

        fun launch(activity: Activity, entry: Entry, readOnly: Boolean, historyPosition: Int? = null) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                val intent = Intent(activity, EntryActivity::class.java)
                intent.putExtra(KEY_ENTRY, entry.nodeId)
                ReadOnlyHelper.putReadOnlyInIntent(intent, readOnly)
                if (historyPosition != null)
                    intent.putExtra(KEY_ENTRY_HISTORY_POSITION, historyPosition)
                activity.startActivityForResult(intent, EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE)
            }
        }
    }
}
