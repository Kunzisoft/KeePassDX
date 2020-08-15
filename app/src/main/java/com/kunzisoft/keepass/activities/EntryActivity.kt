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
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.education.EntryActivityEducation
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.magikeyboard.MagikIME
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.notifications.AttachmentFileNotificationService
import com.kunzisoft.keepass.notifications.ClipboardEntryNotificationService
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_ENTRY_HISTORY
import com.kunzisoft.keepass.notifications.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RESTORE_ENTRY_HISTORY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.utils.createDocument
import com.kunzisoft.keepass.utils.onCreateDocumentResult
import com.kunzisoft.keepass.view.EntryContentsView
import com.kunzisoft.keepass.view.showActionError
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
    private var mAttachmentsToDownload: HashMap<Int, EntryAttachment> = HashMap()

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
        entryProgress = findViewById(R.id.entry_progress)
        lockView = findViewById(R.id.lock_button)

        lockView?.setOnClickListener {
            lockAndExit()
        }

        // Focus view to reinitialize timeout
        resetAppTimeoutWhenViewFocusedOrChanged(coordinatorLayout)

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
            }
            coordinatorLayout?.showActionError(result)
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

            val entryInfo = entry.getEntryInfo(Database.getInstance())

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
                override fun onAttachmentProgress(fileUri: Uri, attachment: EntryAttachment) {
                    entryContentsView?.updateAttachmentDownloadProgress(attachment)
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

        val database = Database.getInstance()
        database.startManageEntry(entry)
        // Assign title icon
        titleIconView?.assignDatabaseIcon(database.drawFactory, entry.icon, iconColor)

        // Assign title text
        val entryTitle = entry.title
        collapsingToolbarLayout?.title = entryTitle
        toolbar?.title = entryTitle

        // Assign basic fields
        entryContentsView?.assignUserName(entry.username)
        entryContentsView?.assignUserNameCopyListener(View.OnClickListener {
            database.startManageEntry(entry)
            clipboardHelper?.timeoutCopyToClipboard(entry.username,
                            getString(R.string.copy_field,
                            getString(R.string.entry_user_name)))
            database.stopManageEntry(entry)
        })

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

        entryContentsView?.assignPassword(entry.password, allowCopyPasswordAndProtectedFields)
        if (allowCopyPasswordAndProtectedFields) {
            entryContentsView?.assignPasswordCopyListener(View.OnClickListener {
                database.startManageEntry(entry)
                clipboardHelper?.timeoutCopyToClipboard(entry.password,
                                getString(R.string.copy_field,
                                getString(R.string.entry_password)))
                database.stopManageEntry(entry)
            })
        } else {
            // If dialog not already shown
            if (isFirstTimeAskAllowCopyPasswordAndProtectedFields) {
                entryContentsView?.assignPasswordCopyListener(showWarningClipboardDialogOnClickListener)
            } else {
                entryContentsView?.assignPasswordCopyListener(null)
            }
        }

        //Assign OTP field
        entryContentsView?.assignOtp(entry.getOtpElement(), entryProgress,
                View.OnClickListener {
                    entry.getOtpElement()?.let { otpElement ->
                        clipboardHelper?.timeoutCopyToClipboard(
                                otpElement.token,
                                getString(R.string.copy_field, getString(R.string.entry_otp))
                        )
                    }
        })

        entryContentsView?.assignURL(entry.url)
        entryContentsView?.assignComment(entry.notes)

        // Assign custom fields
        if (entry.allowCustomFields()) {
            entryContentsView?.clearExtraFields()

            for ((label, value) in entry.customFields) {
                val allowCopyProtectedField = !value.isProtected || allowCopyPasswordAndProtectedFields
                if (allowCopyProtectedField) {
                    entryContentsView?.addExtraField(label, value, allowCopyProtectedField, View.OnClickListener {
                        clipboardHelper?.timeoutCopyToClipboard(
                                value.toString(),
                                getString(R.string.copy_field, label)
                        )
                    })
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
        entryContentsView?.setHiddenPasswordStyle(!mShowPassword)

        // Manage attachments
        entryContentsView?.assignAttachments(entry.getAttachments()) { attachmentItem ->
            when (attachmentItem.downloadState) {
                AttachmentState.NULL, AttachmentState.ERROR, AttachmentState.COMPLETE -> {
                    createDocument(this, attachmentItem.name)?.let { requestCode ->
                        mAttachmentsToDownload[requestCode] = attachmentItem
                    }
                }
                else -> {
                    // TODO Stop download
                }
            }
        }

        // Assign dates
        entryContentsView?.assignCreationDate(entry.creationTime)
        entryContentsView?.assignModificationDate(entry.lastModificationTime)
        entryContentsView?.assignLastAccessDate(entry.lastAccessTime)
        entryContentsView?.setExpires(entry.isCurrentlyExpires)
        if (entry.expires) {
            entryContentsView?.assignExpiresDate(entry.expiryTime)
        } else {
            entryContentsView?.assignExpiresDate(getString(R.string.never))
        }

        // Manage history
        historyView?.visibility = if (mIsHistory) View.VISIBLE else View.GONE
        if (mIsHistory) {
            val taColorAccent = theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
            collapsingToolbarLayout?.contentScrim = ColorDrawable(taColorAccent.getColor(0, Color.BLACK))
            taColorAccent.recycle()
        }
        val entryHistory = entry.getHistory()
        val showHistoryView = entryHistory.isNotEmpty()
        entryContentsView?.showHistory(showHistoryView)
        if (showHistoryView) {
            entryContentsView?.assignHistory(entryHistory)
            entryContentsView?.onHistoryClick { historyItem, position ->
                launch(this, historyItem, mReadOnly, position)
            }
        }
        entryContentsView?.refreshHistory()

        // Assign special data
        entryContentsView?.assignUUID(entry.nodeId.id)

        database.stopManageEntry(entry)
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

    private fun changeShowPasswordIcon(togglePassword: MenuItem?) {
        if (mShowPassword) {
            togglePassword?.setTitle(R.string.menu_hide_password)
            togglePassword?.setIcon(R.drawable.ic_visibility_off_white_24dp)
        } else {
            togglePassword?.setTitle(R.string.menu_showpass)
            togglePassword?.setIcon(R.drawable.ic_visibility_white_24dp)
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

        val togglePassword = menu.findItem(R.id.menu_toggle_pass)
        entryContentsView?.let {
            if (it.isPasswordPresent || it.atLeastOneFieldProtectedPresent()) {
                changeShowPasswordIcon(togglePassword)
            } else {
                togglePassword?.isVisible = false
            }
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
        Handler().post { performedNextEducation(EntryActivityEducation(this), menu) }

        return true
    }

    private fun performedNextEducation(entryActivityEducation: EntryActivityEducation,
                                       menu: Menu) {
        val entryCopyEducationPerformed = entryContentsView?.isUserNamePresent == true
                && entryActivityEducation.checkAndPerformedEntryCopyEducation(
                        findViewById(R.id.entry_user_name_action_image),
                        {
                            clipboardHelper?.timeoutCopyToClipboard(mEntry!!.username,
                                    getString(R.string.copy_field,
                                            getString(R.string.entry_user_name)))
                        },
                        {
                            performedNextEducation(entryActivityEducation, menu)
                        })

        if (!entryCopyEducationPerformed) {
            // entryEditEducationPerformed
            toolbar?.findViewById<View>(R.id.menu_edit) != null && entryActivityEducation.checkAndPerformedEntryEditEducation(
                            toolbar!!.findViewById(R.id.menu_edit),
                            {
                                onOptionsItemSelected(menu.findItem(R.id.menu_edit))
                            },
                            {
                                performedNextEducation(entryActivityEducation, menu)
                            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_contribute -> {
                MenuUtil.onContributionItemSelected(this)
                return true
            }
            R.id.menu_toggle_pass -> {
                mShowPassword = !mShowPassword
                changeShowPasswordIcon(item)
                entryContentsView?.setHiddenPasswordStyle(!mShowPassword)
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
