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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.EntryFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.TagsAdapter
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.UserVerificationActionType
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.checkUserVerification
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.education.EntryActivityEducation
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.services.AttachmentFileNotificationService
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_DELETE_ENTRY_HISTORY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_RESTORE_ENTRY_HISTORY
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.tasks.AttachmentFileBinderManager
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.changeControlColor
import com.kunzisoft.keepass.view.changeTitleColor
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.view.showError
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel
import kotlinx.coroutines.launch
import java.util.EnumSet
import java.util.UUID

class EntryActivity : DatabaseLockActivity() {

    private var container: View? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var collapsingToolbarLayout: CollapsingToolbarLayout? = null
    private var appBarLayout: AppBarLayout? = null
    private var titleIconView: ImageView? = null
    private var historyView: View? = null
    private var tagsListView: RecyclerView? = null
    private var entryContentTab: TabLayout? = null
    private var tagsAdapter: TagsAdapter? = null
    private var entryProgress: LinearProgressIndicator? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var loadingView: ProgressBar? = null
    private var editFab: FloatingActionButton? = null

    private val mEntryViewModel: EntryViewModel by viewModels()
    private val mUserVerificationViewModel: UserVerificationViewModel by viewModels()

    private val mEntryActivityEducation = EntryActivityEducation(this)

    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    private var mExternalFileHelper: ExternalFileHelper? = null
    private var mAttachmentSelected: Attachment? = null

    private var mEntryActivityResultLauncher = EntryEditActivity.registerForEntryResult(this) {
        // Reload the current id from database
        mEntryViewModel.loadDatabase(mDatabase)
    }

    override fun manageDatabaseInfo(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_entry)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get views
        container = findViewById(R.id.activity_entry_container)
        coordinatorLayout = findViewById(R.id.toolbar_coordinator)
        collapsingToolbarLayout = findViewById(R.id.toolbar_layout)
        appBarLayout = findViewById(R.id.app_bar)
        titleIconView = findViewById(R.id.entry_icon)
        historyView = findViewById(R.id.history_container)
        tagsListView = findViewById(R.id.entry_tags_list_view)
        entryContentTab = findViewById(R.id.entry_content_tab)
        entryProgress = findViewById(R.id.entry_progress)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)
        editFab = findViewById(R.id.entry_edit_fab)

        // To apply fit window with transparency
        setTransparentNavigationBar {
            // To fix margin with API 27
            ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbarLayout!!, null)
            container?.applyWindowInsets(EnumSet.of(
                WindowInsetPosition.TOP_MARGINS,
                WindowInsetPosition.BOTTOM_MARGINS,
                WindowInsetPosition.START_MARGINS,
                WindowInsetPosition.END_MARGINS,
            ))
        }

        // Empty title
        collapsingToolbarLayout?.title = " "
        toolbar?.title = " "

        // Set the theme to retrieve the toolbar color
        mEntryViewModel.setTheme(theme)

        // Init Tags adapter
        tagsAdapter = TagsAdapter(this)
        tagsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tagsAdapter
            isFocusable = false
        }

        // Init content tab
        entryContentTab?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                mEntryViewModel.selectSection(EntryViewModel.EntrySection.
                    getEntrySectionByPosition(tab?.position ?: 0)
                )
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Get Entry from UUID
        try {
            intent.getParcelableExtraCompat<NodeId<UUID>>(KEY_ENTRY)?.let { mainEntryId ->
                intent.removeExtra(KEY_ENTRY)
                val historyPosition = intent.getIntExtra(KEY_ENTRY_HISTORY_POSITION, -1)
                intent.removeExtra(KEY_ENTRY_HISTORY_POSITION)

                mEntryViewModel.loadEntry(mDatabase, mainEntryId, historyPosition)
            }
        } catch (_: ClassCastException) {
            Log.e(TAG, "Unable to retrieve the entry key")
        }

        // Init SAF manager
        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildCreateDocument { createdFileUri ->
            mAttachmentSelected?.let { attachment ->
                if (createdFileUri != null) {
                    mAttachmentFileBinderManager
                        ?.startDownloadAttachment(createdFileUri, attachment)
                }
                mAttachmentSelected = null
            }
        }
        // Init attachment service binder manager
        mAttachmentFileBinderManager = AttachmentFileBinderManager(this)

        lockView?.setOnClickListener {
            lockAndExit()
        }

        editFab?.setOnClickListener {
            requestEdition()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mEntryViewModel.entryUIState.collect { entryState ->
                        // Define Loading
                        if (entryState.loaded)
                            loadingView?.hideByFading()
                        else
                            loadingView?.showByFading()
                        val entryInfo = entryState.entryInfo
                        // Assign title text
                        val entryTitle = entryInfo?.title?.ifEmpty { entryInfo.nodeId.toString() } ?: ""
                        collapsingToolbarLayout?.title = entryTitle
                        toolbar?.title = entryTitle
                        // Assign tags
                        val tags = entryInfo?.tags ?: Tags()
                        tagsListView?.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
                        tagsAdapter?.setTags(tags)
                        // Icon
                        titleIconView?.background?.colorFilter = BlendModeColorFilterCompat
                            .createBlendModeColorFilterCompat(entryState.iconBackgroundColor, BlendModeCompat.SRC_IN)
                        entryInfo?.icon?.let { icon ->
                            titleIconView?.let { iconView ->
                                mDatabase?.iconDrawableFactory?.assignDatabaseIcon(
                                    imageView = iconView,
                                    icon = icon,
                                    tintColor = entryState.iconColor
                                )
                            }
                        }
                        // Toolbar customization
                        val toolbarColor = entryState.toolbarColor
                        val onToolbarColor = entryState.onToolbarColor
                        collapsingToolbarLayout?.setBackgroundColor(toolbarColor)
                        collapsingToolbarLayout?.contentScrim = (toolbarColor).toDrawable()
                        toolbar?.changeControlColor(onToolbarColor)
                        collapsingToolbarLayout?.changeTitleColor(onToolbarColor)
                        // Manage FAB visibility
                        editFab?.visibility = if (!mDatabaseReadOnly
                            && entryState.showFloatingActionButton) View.VISIBLE else View.GONE
                        // Assign history dedicated view
                        historyView?.visibility = if (entryState.showHistoryView) View.VISIBLE else View.GONE
                        // Refresh Menu
                        invalidateOptionsMenu()
                    }
                }
                launch {
                    mEntryViewModel.sectionSelected.collect { entrySection ->
                        entryContentTab?.getTabAt(entrySection.position)?.select()
                    }
                }
                launch {
                    mEntryViewModel.onEntryLoaded.collect { onEntryLoaded ->
                        // To sort by access
                        touchEntry(onEntryLoaded.entryInfo)
                    }
                }
                launch {
                    mEntryViewModel.onOtpElementUpdated.collect { otpElement ->
                        if (otpElement == null) {
                            entryProgress?.visibility = View.GONE
                        } else when (otpElement.type) {
                            // Only add token if HOTP
                            OtpType.HOTP -> {
                                entryProgress?.visibility = View.GONE
                            }
                            // Refresh view if TOTP
                            OtpType.TOTP -> {
                                entryProgress?.apply {
                                    max = otpElement.period
                                    setProgressCompat(otpElement.secondsRemaining, true)
                                    visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }
                launch {
                    mEntryViewModel.attachmentSelected.collect { attachmentSelected ->
                        mAttachmentSelected = attachmentSelected
                        mExternalFileHelper?.createDocument(attachmentSelected.name)
                    }
                }
                launch {
                    mEntryViewModel.historySelected.collect { historySelected ->
                        mDatabase?.let { database ->
                            launch(
                                activity = this@EntryActivity,
                                database = database,
                                entryId = historySelected.nodeId,
                                historyPosition = historySelected.historyPosition,
                                activityResultLauncher = mEntryActivityResultLauncher
                            )
                        }
                    }
                }
                launch {
                    mEntryViewModel.requestCopyProtectedField.collect { fieldProtection ->
                        if (mDatabaseAllowUserVerification) {
                            mDatabase?.let { database ->
                                checkUserVerification(
                                    userVerificationViewModel = mUserVerificationViewModel,
                                    dataToVerify = UserVerificationData(
                                        actionType = UserVerificationActionType.COPY_PROTECTED_FIELD,
                                        database = database,
                                        fieldProtection = fieldProtection,
                                    )
                                )
                            }
                        } else {
                            // Copy field value directly without user verification
                            fieldProtection.field.let {
                                mEntryViewModel.copyToClipboard(it)
                            }
                        }
                    }
                }
                launch {
                    mEntryViewModel.onChangeFieldProtectionRequested.collect { fieldProtection ->
                        mDatabase?.let { database ->
                            if (mDatabaseAllowUserVerification) {
                                if (fieldProtection.isCurrentlyProtected) {
                                    checkUserVerification(
                                        userVerificationViewModel = mUserVerificationViewModel,
                                        dataToVerify = UserVerificationData(
                                            actionType = UserVerificationActionType.SHOW_PROTECTED_FIELD,
                                            database = database,
                                            fieldProtection = fieldProtection
                                        )
                                    )
                                } else {
                                    mEntryViewModel.updateProtectionField(
                                        fieldProtection = fieldProtection,
                                        value = true
                                    )
                                }
                            } else {
                                // Toggle field protection directly without user verification
                                mEntryViewModel.updateProtectionField(
                                    fieldProtection = fieldProtection,
                                    value = !fieldProtection.isCurrentlyProtected
                                )
                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mUserVerificationViewModel.onUserVerificationCanceled.collect { result ->
                        coordinatorLayout?.showError(result.error, R.id.entry_content_tab)
                    }
                }
                launch {
                    mUserVerificationViewModel.onUserVerificationSucceeded.collect { data ->
                        when (data.actionType) {
                            UserVerificationActionType.SHOW_PROTECTED_FIELD -> {
                                // Unprotect field by its view
                                data.fieldProtection?.let { field ->
                                    mEntryViewModel.updateProtectionField(
                                        fieldProtection = field,
                                        value = false
                                    )
                                }
                            }
                            UserVerificationActionType.COPY_PROTECTED_FIELD -> {
                                // Copy field value
                                data.fieldProtection?.field?.let {
                                    mEntryViewModel.copyToClipboard(it)
                                }
                            }
                            UserVerificationActionType.EDIT_ENTRY -> {
                                // Edit Entry
                                editEntry(data.database, data.entryId)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun finishActivityIfReloadRequested(): Boolean = true

    override fun viewToInvalidateTimeout(): View? {
        return coordinatorLayout
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)
        mEntryViewModel.loadDatabase(database)
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_RESTORE_ENTRY_HISTORY,
            ACTION_DATABASE_DELETE_ENTRY_HISTORY -> {
                // Close the current activity after a history action
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

        // Keep the screen on
        if (PreferencesUtil.isKeepScreenOnEnabled(this)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        mAttachmentFileBinderManager?.unregisterProgressTask()

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (mEntryViewModel.entryLoaded) {
            val inflater = menuInflater
            inflater.inflate(R.menu.database, menu)

            if (mEntryViewModel.entryIsHistory && !mDatabaseReadOnly) {
                inflater.inflate(R.menu.entry_history, menu)
            }

            // Show education views
            Handler(Looper.getMainLooper()).post {
                performedNextEducation(menu)
            }
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (mEntryViewModel.entryIsHistory || mDatabaseReadOnly) {
            menu?.findItem(R.id.menu_save_database)?.isVisible = false
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
        }
        if (!mMergeDataAllowed) {
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
        }
        if (mSpecialMode != SpecialMode.DEFAULT) {
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
            menu?.findItem(R.id.menu_reload_database)?.isVisible = false
        }
        mEntryViewModel.applyToolbarColors()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun performedNextEducation(menu: Menu) {
        val entryFragment = supportFragmentManager.findFragmentByTag(ENTRY_FRAGMENT_TAG)
                as? EntryFragment?
        val entryFieldCopyView: View? = entryFragment?.firstEntryFieldCopyView()
        val entryCopyEducationPerformed = entryFieldCopyView != null
                && mEntryActivityEducation.checkAndPerformedEntryCopyEducation(
                entryFieldCopyView,
                {
                    entryFragment.launchEntryCopyEducationAction()
                },
                {
                    performedNextEducation(menu)
                })

        if (!entryCopyEducationPerformed) {
            val menuEditView = editFab
            // entryEditEducationPerformed
            menuEditView != null && mEntryActivityEducation.checkAndPerformedEntryEditEducation(
                    menuEditView,
                    {
                        requestEdition()
                    },
                    {
                        performedNextEducation(menu)
                    }
            )
        }
    }

    private fun requestEdition() {
        if (mDatabaseAllowUserVerification) {
            mDatabase?.let { database ->
                checkUserVerification(
                    userVerificationViewModel = mUserVerificationViewModel,
                    dataToVerify = UserVerificationData(
                        actionType = UserVerificationActionType.EDIT_ENTRY,
                        database = database,
                        entryId = mEntryViewModel.mainEntryId
                    )
                )
            }
        } else {
            editEntry(mDatabase, mEntryViewModel.mainEntryId)
        }
    }

    private fun editEntry(database: ContextualDatabase?, entryId: NodeId<*>?) {
        database?.let { database ->
            entryId?.let { entryId ->
                EntryEditActivity.launch(
                    activity = this@EntryActivity,
                    database = database,
                    registrationType = EntryEditActivity.RegistrationType.UPDATE,
                    nodeId = entryId,
                    activityResultLauncher = mEntryActivityResultLauncher
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_restore_entry_history -> {
                mEntryViewModel.mainEntryId?.let { mainEntryId ->
                    restoreEntryHistory(
                        mainEntryId,
                        mEntryViewModel.historyPosition
                    )
                }
            }
            R.id.menu_delete_entry_history -> {
                mEntryViewModel.mainEntryId?.let { mainEntryId ->
                    deleteEntryHistory(
                        mainEntryId,
                        mEntryViewModel.historyPosition
                    )
                }
            }
            R.id.menu_save_database -> {
                saveDatabase()
            }
            R.id.menu_merge_database -> {
                mergeDatabase()
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
            putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mEntryViewModel.mainEntryId)
            setResult(RESULT_OK, this)
        }
        super.finish()
    }

    companion object {
        private val TAG = EntryActivity::class.java.name

        const val KEY_ENTRY = "KEY_ENTRY"
        const val KEY_ENTRY_HISTORY_POSITION = "KEY_ENTRY_HISTORY_POSITION"

        const val ENTRY_FRAGMENT_TAG = "ENTRY_FRAGMENT_TAG"

        /**
         * Open standard or history Entry activity
         */
        fun launch(
            activity: Activity,
            database: ContextualDatabase,
            entryId: NodeId<UUID>,
            historyPosition: Int? = null,
            activityResultLauncher: ActivityResultLauncher<Intent>
        ) {
            if (database.loaded) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    historyPosition?.let {
                        intent.putExtra(KEY_ENTRY_HISTORY_POSITION, historyPosition)
                    }
                    activityResultLauncher.launch(intent)
                }
            }
        }
    }
}
