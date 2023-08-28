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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.fragments.EntryFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.TagsAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
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
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.changeControlColor
import com.kunzisoft.keepass.view.changeTitleColor
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import java.util.UUID

class EntryActivity : DatabaseLockActivity() {

    private var footer: ViewGroup? = null
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

    private val mEntryViewModel: EntryViewModel by viewModels()

    private val mEntryActivityEducation = EntryActivityEducation(this)

    private var mMainEntryId: NodeId<UUID>? = null
    private var mHistoryPosition: Int = -1
    private var mEntryIsHistory: Boolean = false
    private var mEntryLoaded = false

    private var mAttachmentFileBinderManager: AttachmentFileBinderManager? = null
    private var mExternalFileHelper: ExternalFileHelper? = null
    private var mAttachmentSelected: Attachment? = null

    private var mEntryActivityResultLauncher = EntryEditActivity.registerForEntryResult(this) {
        // Reload the current id from database
        mEntryViewModel.loadDatabase(mDatabase)
    }

    private var mIcon: IconImage? = null
    private var mColorSecondary: Int = 0
    private var mColorSurface: Int = 0
    private var mColorOnSurface: Int = 0
    private var mColorBackground: Int = 0
    private var mBackgroundColor: Int? = null
    private var mForegroundColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_entry)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get views
        footer = findViewById(R.id.activity_entry_footer)
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

        // To apply fit window with transparency
        setTransparentNavigationBar {
            // To fix margin with API 27
            ViewCompat.setOnApplyWindowInsetsListener(collapsingToolbarLayout!!, null)
            coordinatorLayout?.applyWindowInsets(WindowInsetPosition.TOP)
            footer?.applyWindowInsets(WindowInsetPosition.BOTTOM)
        }

        // Empty title
        collapsingToolbarLayout?.title = " "
        toolbar?.title = " "

        // Retrieve the textColor to tint the toolbar
        val taColorSecondary = theme.obtainStyledAttributes(intArrayOf(R.attr.colorSecondary))
        val taColorSurface = theme.obtainStyledAttributes(intArrayOf(R.attr.colorSurface))
        val taColorOnSurface = theme.obtainStyledAttributes(intArrayOf(R.attr.colorOnSurface))
        val taColorBackground = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        mColorSecondary = taColorSecondary.getColor(0, Color.BLACK)
        mColorSurface = taColorSurface.getColor(0, Color.BLACK)
        mColorOnSurface = taColorOnSurface.getColor(0, Color.BLACK)
        mColorBackground = taColorBackground.getColor(0, Color.BLACK)
        taColorSecondary.recycle()
        taColorSurface.recycle()
        taColorOnSurface.recycle()
        taColorBackground.recycle()

        // Init Tags adapter
        tagsAdapter = TagsAdapter(this)
        tagsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tagsAdapter
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
        } catch (e: ClassCastException) {
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

        mEntryViewModel.sectionSelected.observe(this) { entrySection ->
            entryContentTab?.getTabAt(entrySection.position)?.select()
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
                // TODO History badge
                /*
                if (entryIsHistory) {
                }*/

                val entryInfo = entryInfoHistory.entryInfo
                // Manage entry copy to start notification if allowed (at the first start)
                if (savedInstanceState == null) {
                    // Manage entry to launch copying notification if allowed
                    ClipboardEntryNotificationService.checkAndLaunchNotification(this, entryInfo)
                    // Manage entry to populate Magikeyboard and launch keyboard notification if allowed
                    if (PreferencesUtil.isKeyboardEntrySelectionEnable(this)) {
                        MagikeyboardService.addEntryAndLaunchNotificationIfAllowed(this, entryInfo)
                    }
                }
                // Assign title icon
                mIcon = entryInfo.icon
                // Assign title text
                val entryTitle =
                    if (entryInfo.title.isNotEmpty()) entryInfo.title else UuidUtil.toHexString(entryInfo.id)
                collapsingToolbarLayout?.title = entryTitle
                toolbar?.title = entryTitle
                // Assign tags
                val tags = entryInfo.tags
                tagsListView?.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
                tagsAdapter?.setTags(tags)
                // Assign colors
                val showEntryColors = PreferencesUtil.showEntryColors(this)
                mBackgroundColor = if (showEntryColors) entryInfo.backgroundColor else null
                mForegroundColor = if (showEntryColors) entryInfo.foregroundColor else null

                loadingView?.hideByFading()
                mEntryLoaded = true
            } else {
                finish()
            }
            // Refresh Menu
            invalidateOptionsMenu()
        }

        mEntryViewModel.onOtpElementUpdated.observe(this) { otpElement ->
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

        mEntryViewModel.attachmentSelected.observe(this) { attachmentSelected ->
            mAttachmentSelected = attachmentSelected
            mExternalFileHelper?.createDocument(attachmentSelected.name)
        }

        mEntryViewModel.historySelected.observe(this) { historySelected ->
            mDatabase?.let { database ->
                launch(
                    this,
                    database,
                    historySelected.nodeId,
                    historySelected.historyPosition,
                    mEntryActivityResultLauncher
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

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
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

        // Keep the screen on
        if (PreferencesUtil.isKeepScreenOnEnabled(this)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        mAttachmentFileBinderManager?.unregisterProgressTask()

        super.onPause()
    }

    private fun applyToolbarColors() {
        collapsingToolbarLayout?.setBackgroundColor(mBackgroundColor ?: mColorSurface)
        collapsingToolbarLayout?.contentScrim = ColorDrawable(mBackgroundColor ?: mColorSurface)
        val backgroundDarker = if (mBackgroundColor != null) {
            ColorUtils.blendARGB(mBackgroundColor!!, Color.WHITE, 0.1f)
        } else {
            mColorBackground
        }
        titleIconView?.background?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(backgroundDarker, BlendModeCompat.SRC_IN)
        mIcon?.let { icon ->
            titleIconView?.let { iconView ->
                mDatabase?.iconDrawableFactory?.assignDatabaseIcon(
                    iconView,
                    icon,
                    mForegroundColor ?: mColorSecondary
                )
            }
        }
        toolbar?.changeControlColor(mForegroundColor ?: mColorOnSurface)
        collapsingToolbarLayout?.changeTitleColor(mForegroundColor ?: mColorOnSurface)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (mEntryLoaded) {
            val inflater = menuInflater

            inflater.inflate(R.menu.entry, menu)
            inflater.inflate(R.menu.database, menu)

            if (mEntryIsHistory && !mDatabaseReadOnly) {
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
        if (mEntryIsHistory || mDatabaseReadOnly) {
            menu?.findItem(R.id.menu_save_database)?.isVisible = false
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
            menu?.findItem(R.id.menu_edit)?.isVisible = false
        }
        if (!mMergeDataAllowed) {
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
        }
        if (mSpecialMode != SpecialMode.DEFAULT) {
            menu?.findItem(R.id.menu_merge_database)?.isVisible = false
            menu?.findItem(R.id.menu_reload_database)?.isVisible = false
        }
        applyToolbarColors()
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
            val menuEditView = toolbar?.findViewById<View>(R.id.menu_edit)
            // entryEditEducationPerformed
            menuEditView != null && mEntryActivityEducation.checkAndPerformedEntryEditEducation(
                    menuEditView,
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_edit))
                    },
                    {
                        performedNextEducation(menu)
                    }
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                mDatabase?.let { database ->
                    mMainEntryId?.let { entryId ->
                        EntryEditActivity.launchToUpdate(
                            this,
                            database,
                            entryId,
                            mEntryActivityResultLauncher
                        )
                    }
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
            putExtra(EntryEditActivity.ADD_OR_UPDATE_ENTRY_KEY, mMainEntryId)
            setResult(Activity.RESULT_OK, this)
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
                   database: ContextualDatabase,
                   entryId: NodeId<UUID>,
                   activityResultLauncher: ActivityResultLauncher<Intent>) {
            if (database.loaded) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    activityResultLauncher.launch(intent)
                }
            }
        }

        /**
         * Open history Entry activity
         */
        fun launch(activity: Activity,
                   database: ContextualDatabase,
                   entryId: NodeId<UUID>,
                   historyPosition: Int,
                   activityResultLauncher: ActivityResultLauncher<Intent>) {
            if (database.loaded) {
                if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                    val intent = Intent(activity, EntryActivity::class.java)
                    intent.putExtra(KEY_ENTRY, entryId)
                    intent.putExtra(KEY_ENTRY_HISTORY_POSITION, historyPosition)
                    activityResultLauncher.launch(intent)
                }
            }
        }
    }
}
