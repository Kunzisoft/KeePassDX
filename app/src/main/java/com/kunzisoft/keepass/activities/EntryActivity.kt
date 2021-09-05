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
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.EntryFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.education.EntryActivityEducation
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.services.AttachmentFileNotificationService
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_ENTRY_HISTORY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RESTORE_ENTRY_HISTORY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.*
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import java.util.*
import kotlin.collections.HashMap

class EntryActivity : DatabaseLockActivity() {

    private var coordinatorLayout: CoordinatorLayout? = null
    private var collapsingToolbarLayout: CollapsingToolbarLayout? = null
    private var titleIconView: ImageView? = null
    private var historyView: View? = null
    private var entryProgress: ProgressBar? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var loadingView: ProgressBar? = null

    private val mEntryViewModel: EntryViewModel by viewModels()

    private var mMainEntryId: NodeId<UUID>? = null
    private var mHistoryPosition: Int = -1
    private var mEntryIsHistory: Boolean = false
    private var mUrl: String? = null
    private var mEntryLoaded = false

    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    private var mAttachmentsToDownload: HashMap<Int, Attachment> = HashMap()
    private var mExternalFileHelper: ExternalFileHelper? = null

    private var mIcon: IconImage? = null
    private var mIconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_entry)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get views
        coordinatorLayout = findViewById(R.id.toolbar_coordinator)
        collapsingToolbarLayout = findViewById(R.id.toolbar_layout)
        titleIconView = findViewById(R.id.entry_icon)
        historyView = findViewById(R.id.history_container)
        entryProgress = findViewById(R.id.entry_progress)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)

        // Empty title
        collapsingToolbarLayout?.title = " "
        toolbar?.title = " "

        // Retrieve the textColor to tint the icon
        val taIconColor = theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
        mIconColor = taIconColor.getColor(0, Color.BLACK)
        taIconColor.recycle()

        // Get Entry from UUID
        try {
            intent.getParcelableExtra<NodeId<UUID>?>(KEY_ENTRY)?.let { mainEntryId ->
                intent.removeExtra(KEY_ENTRY)
                val historyPosition = intent.getIntExtra(KEY_ENTRY_HISTORY_POSITION, -1)
                intent.removeExtra(KEY_ENTRY_HISTORY_POSITION)

                mEntryViewModel.loadEntry(mDatabase, mainEntryId, historyPosition)
            }
        } catch (e: ClassCastException) {
            Log.e(TAG, "Unable to retrieve the entry key")
        }

        // Init SAF manager
        mExternalFileHelper = ExternalFileHelper(this)
        // Init attachment service binder manager
        mAttachmentFileBinderManager = AttachmentFileBinderManager(this)

        lockView?.setOnClickListener {
            lockAndExit()
        }

        mEntryViewModel.entryInfoHistory.observe(this) { entryInfoHistory ->
            if (entryInfoHistory != null) {
                this.mMainEntryId = entryInfoHistory.mainEntryId

                // Manage history position
                val historyPosition = entryInfoHistory.historyPosition
                this.mHistoryPosition = historyPosition
                val entryIsHistory = historyPosition > -1
                this.mEntryIsHistory = entryIsHistory
                // Assign history dedicated view
                historyView?.visibility = if (entryIsHistory) View.VISIBLE else View.GONE
                if (entryIsHistory) {
                    val taColorAccent = theme.obtainStyledAttributes(intArrayOf(R.attr.colorAccent))
                    collapsingToolbarLayout?.contentScrim =
                        ColorDrawable(taColorAccent.getColor(0, Color.BLACK))
                    taColorAccent.recycle()
                }

                val entryInfo = entryInfoHistory.entryInfo
                // Manage entry copy to start notification if allowed (at the first start)
                if (savedInstanceState == null) {
                    // Manage entry to launch copying notification if allowed
                    ClipboardEntryNotificationService.launchNotificationIfAllowed(this, entryInfo)
                    // Manage entry to populate Magikeyboard and launch keyboard notification if allowed
                    if (PreferencesUtil.isKeyboardEntrySelectionEnable(this)) {
                        MagikeyboardService.addEntryAndLaunchNotificationIfAllowed(this, entryInfo)
                    }
                }
                // Assign title icon
                mIcon = entryInfo.icon
                titleIconView?.let { iconView ->
                    mIconDrawableFactory?.assignDatabaseIcon(iconView, entryInfo.icon, mIconColor)
                }
                // Assign title text
                val entryTitle =
                    if (entryInfo.title.isNotEmpty()) entryInfo.title else entryInfo.id.toString()
                collapsingToolbarLayout?.title = entryTitle
                toolbar?.title = entryTitle
                mUrl = entryInfo.url

                loadingView?.hideByFading()
                mEntryLoaded = true
            } else {
                finish()
            }
            // Refresh Menu
            invalidateOptionsMenu()
        }

        mEntryViewModel.onOtpElementUpdated.observe(this) { otpElement ->
            if (otpElement == null)
                entryProgress?.visibility = View.GONE
            when (otpElement?.type) {
                // Only add token if HOTP
                OtpType.HOTP -> {
                    entryProgress?.visibility = View.GONE
                }
                // Refresh view if TOTP
                OtpType.TOTP -> {
                    entryProgress?.apply {
                        max = otpElement.period
                        progress = otpElement.secondsRemaining
                        visibility = View.VISIBLE
                    }
                }
            }
        }

        mEntryViewModel.attachmentSelected.observe(this) { attachmentSelected ->
            mExternalFileHelper?.createDocument(attachmentSelected.name)?.let { requestCode ->
                mAttachmentsToDownload[requestCode] = attachmentSelected
            }
        }

        mEntryViewModel.historySelected.observe(this) { historySelected ->
            mDatabase?.let { database ->
                launch(
                    this,
                    database,
                    historySelected.nodeId,
                    historySelected.historyPosition
                )
            }
        }
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)

        mEntryViewModel.loadDatabase(database)

        // Assign title icon
        mIcon?.let { icon ->
            titleIconView?.let { iconView ->
                mIconDrawableFactory?.assignDatabaseIcon(iconView, icon, mIconColor)
            }
        }
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_RESTORE_ENTRY_HISTORY,
            ACTION_DATABASE_DELETE_ENTRY_HISTORY -> {
                // Close the current activity after an history action
                if (result.isSuccess)
                    finish()
            }
        }
        coordinatorLayout?.showActionErrorIfNeeded(result)
    }

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        mAttachmentFileBinderManager?.apply {
            registerProgressTask()
            onActionTaskListener = object : AttachmentFileNotificationService.ActionTaskListener {
                override fun onAttachmentAction(fileUri: Uri, entryAttachmentState: EntryAttachmentState) {
                    mEntryViewModel.onAttachmentAction(entryAttachmentState)
                }
            }
        }
    }

    override fun onPause() {
        mAttachmentFileBinderManager?.unregisterProgressTask()

        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE -> {
                // Reload the current id from database
                mEntryViewModel.loadDatabase(mDatabase)
            }
        }

        mExternalFileHelper?.onCreateDocumentResult(requestCode, resultCode, data) { createdFileUri ->
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
        if (mEntryLoaded) {
            val inflater = menuInflater
            MenuUtil.contributionMenuInflater(inflater, menu)

            inflater.inflate(R.menu.entry, menu)
            inflater.inflate(R.menu.database, menu)

            if (mEntryIsHistory && !mDatabaseReadOnly) {
                inflater.inflate(R.menu.entry_history, menu)
            }

            // Show education views
            Handler(Looper.getMainLooper()).post {
                performedNextEducation(
                    EntryActivityEducation(
                        this
                    ), menu
                )
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (mUrl?.isEmpty() != false) {
            menu?.findItem(R.id.menu_goto_url)?.isVisible = false
        }
        if (mEntryIsHistory || mDatabaseReadOnly) {
            menu?.findItem(R.id.menu_save_database)?.isVisible = false
            menu?.findItem(R.id.menu_edit)?.isVisible = false
        }
        if (mSpecialMode != SpecialMode.DEFAULT) {
            menu?.findItem(R.id.menu_reload_database)?.isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun performedNextEducation(entryActivityEducation: EntryActivityEducation,
                                       menu: Menu) {
        val entryFragment = supportFragmentManager.findFragmentByTag(ENTRY_FRAGMENT_TAG)
                as? EntryFragment?
        val entryFieldCopyView: View? = entryFragment?.firstEntryFieldCopyView()
        val entryCopyEducationPerformed = entryFieldCopyView != null
                && entryActivityEducation.checkAndPerformedEntryCopyEducation(
                entryFieldCopyView,
                {
                    entryFragment.launchEntryCopyEducationAction()
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
                mDatabase?.let { database ->
                    mMainEntryId?.let { entryId ->
                        EntryEditActivity.launchToUpdate(
                            this,
                            database,
                            entryId
                        )
                    }
                }
                return true
            }
            R.id.menu_goto_url -> {
                mUrl?.let { url ->
                    UriUtil.gotoUrl(this, url)
                }
                return true
            }
            R.id.menu_restore_entry_history -> {
                mMainEntryId?.let { mainEntryId ->
                    restoreEntryHistory(
                        mainEntryId,
                        mHistoryPosition)
                }
            }
            R.id.menu_delete_entry_history -> {
                mMainEntryId?.let { mainEntryId ->
                    deleteEntryHistory(
                        mainEntryId,
                        mHistoryPosition)
                }
            }
            R.id.menu_save_database -> {
                saveDatabase()
            }
            R.id.menu_reload_database -> {
                reloadDatabase()
            }
            android.R.id.home -> finish() // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        // Transit data in previous Activity after an update
        Intent().apply {
            putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mMainEntryId)
            setResult(EntryEditActivity.ADD_OR_UPDATE_ENTRY_RESULT_CODE, this)
        }
        super.finish()
    }

    companion object {
        private val TAG = EntryActivity::class.java.name

        const val KEY_ENTRY = "KEY_ENTRY"
        const val KEY_ENTRY_HISTORY_POSITION = "KEY_ENTRY_HISTORY_POSITION"

        const val ENTRY_FRAGMENT_TAG = "ENTRY_FRAGMENT_TAG"

        /**
         * Open standard Entry activity
         */
        fun launch(activity: Activity,
                   database: Database,
                   entryId: NodeId<UUID>) {
            if (database.loaded) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    activity.startActivityForResult(
                        intent,
                        EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE
                    )
                }
            }
        }

        /**
         * Open history Entry activity
         */
        fun launch(activity: Activity,
                   database: Database,
                   entryId: NodeId<UUID>,
                   historyPosition: Int) {
            if (database.loaded) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    intent.putExtra(KEY_ENTRY_HISTORY_POSITION, historyPosition)
                    activity.startActivityForResult(
                        intent,
                        EntryEditActivity.ADD_OR_UPDATE_ENTRY_REQUEST_CODE
                    )
                }
            }
        }
    }
}
