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
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
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
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker.Builder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupDialogFragment
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment
import com.kunzisoft.keepass.activities.dialogs.MainCredentialDialogFragment
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.fragments.GroupFragment
import com.kunzisoft.keepass.activities.fragments.SearchFragment
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.BreadcrumbAdapter
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.buildSpecialModeResponseAndSetResult
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeModes
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.UserVerificationActionType
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.checkUserVerification
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.buildPasskeyResponseAndSetResult
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.buildPasswordResponseAndSetResult
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.model.DataTime
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_COPY_NODES_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_MOVE_NODES_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_TOUCH_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_TOUCH_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getNewEntry
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.KeyboardUtil.hideKeyboard
import com.kunzisoft.keepass.utils.TimeUtil.datePickerToDataDate
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.view.AddNodeButtonView
import com.kunzisoft.keepass.view.NavigationDatabaseView
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.view.showError
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.view.updateButtonPaddingStart
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel.NodeActionMode
import com.kunzisoft.keepass.viewmodels.MainCredentialDialogViewModel
import com.kunzisoft.keepass.viewmodels.NodeEditViewModel
import com.kunzisoft.keepass.viewmodels.SearchViewModel
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.joda.time.LocalDateTime
import java.util.EnumSet


class GroupActivity : DatabaseLockActivity() {

    // Views
    private var header: ViewGroup? = null
    private var footer: ViewGroup? = null
    private var drawerLayout: DrawerLayout? = null
    private var constraintLayout: ConstraintLayout? = null
    private var databaseNavView: NavigationDatabaseView? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var coordinatorError: CoordinatorLayout? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var databaseModifiedView: ImageView? = null
    private var databaseColorView: ImageView? = null
    private var databaseNameView: TextView? = null
    private var toolbarAction: ToolbarAction? = null
    private var numberChildrenView: TextView? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var breadcrumbListView: RecyclerView? = null
    private var searchView: ViewGroup? = null
    private var loadingView: ProgressBar? = null

    private val mGroupViewModel: GroupViewModel by viewModels()
    private val mGroupEditViewModel: GroupEditViewModel by viewModels()
    private val mSearchViewModel: SearchViewModel by viewModels()
    private val mMainCredentialDialogViewModel: MainCredentialDialogViewModel by viewModels()
    private val mUserVerificationViewModel: UserVerificationViewModel by viewModels()

    private val mGroupActivityEducation = GroupActivityEducation(this)

    private var mBreadcrumbAdapter: BreadcrumbAdapter? = null

    private var activeActionMode: ActionMode? = null

    // Manage merge
    private var mExternalFileHelper: ExternalFileHelper? = null


    private val mEntryActivityResultLauncher = EntryEditActivity.registerForEntryResult(this) { entryId ->
        entryId?.let {
            // Simply refresh the list when entry is updated
            loadGroup()
        } ?: Log.e(this.javaClass.name, "Entry cannot be retrieved in Activity Result")
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mGroupViewModel.onActionCreated()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val database = mDatabase ?: return false
            val state = mGroupViewModel.nodeActionState.value
            val nodes = state.selectedNodes

            menu?.clear()
            when (state.nodeActionMode) {
                NodeActionMode.SELECTION -> {
                    mode?.menuInflater?.inflate(R.menu.node_menu, menu)

                    // Open and Edit for a single item
                    if (nodes.size == 1) {
                        // Edition
                        if (database.isReadOnly || mGroupViewModel.containsRecycleBin(
                                database, nodes.filterIsInstance<NodeInfo>())) {
                            menu?.removeItem(R.id.menu_edit)
                        }
                    } else {
                        menu?.removeItem(R.id.menu_open)
                        menu?.removeItem(R.id.menu_edit)
                    }

                    // Move
                    if (database.isReadOnly) {
                        menu?.removeItem(R.id.menu_move)
                    }

                    // Copy (not allowed for group)
                    if (database.isReadOnly
                        || nodes.any { it is GroupInfo }) {
                        menu?.removeItem(R.id.menu_copy)
                    }

                    // Deletion
                    if (database.isReadOnly || mGroupViewModel.containsRecycleBin(
                            database, nodes.filterIsInstance<NodeInfo>())) {
                        menu?.removeItem(R.id.menu_delete)
                    }
                }
                NodeActionMode.PASTE_FROM_COPY,
                NodeActionMode.PASTE_FROM_MOVE -> {
                    mode?.menuInflater?.inflate(R.menu.node_paste_menu, menu)
                }
                null -> {}
            }

            // Add the number of items selected in title
            mode?.title = nodes.size.toString()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.menu_open -> {
                    mGroupViewModel.onOpenNodeClicked()
                    true
                }
                R.id.menu_edit -> {
                    mGroupViewModel.onEditNodeClicked()
                    true
                }
                R.id.menu_copy -> {
                    mGroupViewModel.onCopyNodesClicked()
                    true
                }
                R.id.menu_move -> {
                    mGroupViewModel.onMoveNodesClicked()
                    true
                }
                R.id.menu_delete -> {
                    mGroupViewModel.onDeleteNodesClicked()
                    true
                }
                R.id.menu_paste -> {
                    mGroupViewModel.onPasteNodesClicked()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mGroupViewModel.onActionDestroyed()
            activeActionMode = null
        }
    }

    private var mIconSelectionActivityResultLauncher = IconPickerActivity.registerIconSelectionForResult(this) { icon ->
        // To create tree dialog for icon
        mGroupEditViewModel.selectIcon(icon)
    }

    override fun manageDatabaseInfo(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.activity_group, null))

        // Initialize views
        header = findViewById(R.id.activity_group_header)
        footer = findViewById(R.id.activity_group_footer)
        drawerLayout = findViewById(R.id.drawer_layout)
        constraintLayout = findViewById(R.id.activity_group_container_view)
        databaseNavView = findViewById(R.id.database_nav_view)
        coordinatorLayout = findViewById(R.id.group_coordinator)
        coordinatorError = findViewById(R.id.error_coordinator)
        numberChildrenView = findViewById(R.id.group_numbers)
        addNodeButtonView = findViewById(R.id.add_node_button)
        toolbar = findViewById(R.id.toolbar)
        databaseModifiedView = findViewById(R.id.database_modified)
        databaseColorView = findViewById(R.id.database_color)
        databaseNameView = findViewById(R.id.database_name)
        breadcrumbListView = findViewById(R.id.breadcrumb_list)
        searchView = findViewById(R.id.search_content_view)
        toolbarAction = findViewById(R.id.toolbar_action)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)

        // To apply fit window with transparency
        setTransparentNavigationBar(applyToStatusBar = true) {
            constraintLayout?.applyWindowInsets(EnumSet.of(
                WindowInsetPosition.TOP_MARGINS,
                WindowInsetPosition.BOTTOM_MARGINS,
                WindowInsetPosition.START_MARGINS,
                WindowInsetPosition.END_MARGINS,
            ))
            // The background of the drawer is meant to overlap system bars, so use padding
            databaseNavView?.applyWindowInsets(EnumSet.of(
                WindowInsetPosition.TOP_PADDING,
                WindowInsetPosition.BOTTOM_PADDING,
                // Only on the start side, since the drawer is anchored to one side of the screen
                WindowInsetPosition.START_PADDING,
            ))
        }

        lockView?.setOnClickListener {
            lockAndExit()
        }

        toolbar?.title = ""
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        // Manage "merge from" and "save to"
        mExternalFileHelper = ExternalFileHelper(this)
        mExternalFileHelper?.buildOpenDocument { uri ->
            launchDialogToAskMainCredential(uri)
        }
        mExternalFileHelper?.buildCreateDocument("application/x-keepass") { uri ->
            uri?.let {
                saveDatabaseTo(it)
            }
        }

        // Menu in drawer
        databaseNavView?.apply {
            inflateMenu(R.menu.settings)
            inflateMenu(R.menu.database_extra)
            inflateMenu(R.menu.about)
            setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_app_settings -> {
                        // To avoid flickering when launch settings in a LockingActivity
                        SettingsActivity.launch(this@GroupActivity, true)
                    }
                    R.id.menu_merge_from -> {
                        if (mDatabaseAllowUserVerification) {
                            checkUserVerification(
                                userVerificationViewModel = mUserVerificationViewModel,
                                dataToVerify = UserVerificationData(
                                    actionType = UserVerificationActionType.MERGE_FROM_DATABASE,
                                    database = mDatabase
                                )
                            )
                        } else {
                            // Open document picker directly without verification
                            mExternalFileHelper?.openDocument()
                        }
                    }
                    R.id.menu_save_copy_to -> {
                        if (mDatabaseAllowUserVerification) {
                            checkUserVerification(
                                userVerificationViewModel = mUserVerificationViewModel,
                                dataToVerify = UserVerificationData(
                                    actionType = UserVerificationActionType.SAVE_DATABASE_COPY_TO,
                                    database = mDatabase
                                )
                            )
                        } else {
                            // Create document directly without verification
                            mExternalFileHelper?.createDocument(
                                getString(R.string.database_file_name_default) +
                                        "_" +
                                        LocalDateTime.now().toString() +
                                        mDatabase?.defaultFileExtension
                            )
                        }
                    }
                    R.id.menu_lock_all -> {
                        lockAndExit()
                    }
                    R.id.menu_contribute -> {
                        this@GroupActivity.openUrl(R.string.contribution_url)
                    }
                    R.id.menu_about -> {
                        startActivity(Intent(this@GroupActivity, AboutActivity::class.java))
                    }
                }
                false
            }
        }

        mGroupViewModel.assignPreferences()
        // Retrieve group if defined at launch
        manageIntent(intent)

        // Breadcrumb
        mBreadcrumbAdapter = BreadcrumbAdapter(this)

        // Initialize the fragment with the list
        var groupFragment =
            supportFragmentManager.findFragmentByTag(GROUP_FRAGMENT_TAG) as GroupFragment?
        if (groupFragment == null)
            groupFragment = GroupFragment()
        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
            R.id.nodes_list_fragment_container,
            groupFragment,
            GROUP_FRAGMENT_TAG
        ).commit()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (mSearchViewModel.isSearchActivated)
                    showSearch()
                launch {
                    mGroupViewModel.groupUIState.collect { state ->
                        // Load
                        if (state.loaded)
                            loadingView?.hideByFading()
                        else
                            loadingView?.showByFading()
                        // Show Buttons
                        addNodeButtonView?.closeButtonIfOpen()
                        addNodeButtonView?.enableAddGroup(state.showAddGroupButton)
                        addNodeButtonView?.enableAddEntry(state.showAddGroupButton)
                        if (state.showAddNodeButton)
                            addNodeButtonView?.showButton()
                        else
                            addNodeButtonView?.hideButton()
                        invalidateOptionsMenu()
                    }
                }
                launch {
                    mGroupViewModel.groupUIState
                        .mapNotNull { it.breadcrumbs }
                        .distinctUntilChanged()
                        .collect { breadcrumbs ->
                            // Show BreadCrumbs
                            mBreadcrumbAdapter?.setNode(breadcrumbs)
                            breadcrumbListView?.scrollToPosition(breadcrumbs.size -1)
                        }
                }
                launch {
                    mGroupViewModel.groupUIState
                        .mapNotNull { it.group }
                        .distinctUntilChanged()
                        .collect { group ->
                            // Show current group
                            refreshDatabaseViews()
                            touchGroup(group)
                            mSearchViewModel.onMainGroupLoaded(mGroupViewModel.mainGroup)
                        }
                }
                launch {
                    mGroupViewModel.viewEvent.collect { event ->
                        when (event) {
                            is GroupViewModel.GroupEvent.OpenGroup -> {
                                mGroupViewModel.loadGroup(event.groupId)
                            }
                            is GroupViewModel.GroupEvent.OpenEntry -> {
                                mDatabase?.let { database ->
                                    val entry = event.entry
                                    EntrySelectionHelper.doSpecialAction(
                                        intent = intent,
                                        defaultAction = {
                                            EntryActivity.launch(
                                                activity = this@GroupActivity,
                                                database = database,
                                                entryId = entry.nodeId,
                                                activityResultLauncher = mEntryActivityResultLauncher
                                            )
                                            // Do not reload group here
                                        },
                                        searchAction = {
                                            // Nothing here, a search is simply performed
                                        },
                                        selectionAction = { _, typeMode, searchInfo ->
                                            when (typeMode) {
                                                TypeMode.DEFAULT -> {}
                                                TypeMode.MAGIKEYBOARD -> {
                                                    if (!database.isReadOnly
                                                        && entry.allowedToSaveSearchInfo(searchInfo)
                                                        && PreferencesUtil.isKeyboardSaveSearchInfoEnable(
                                                            this@GroupActivity
                                                        )
                                                    ) {
                                                        updateEntryWithRegisterInfo(
                                                            database = database,
                                                            entry = entry,
                                                            registerInfo = searchInfo!!.toRegisterInfo()
                                                        )
                                                    } else {
                                                        entrySelectedForSelection(entry)
                                                    }
                                                }

                                                TypeMode.AUTOFILL -> {
                                                    if (!database.isReadOnly
                                                        && entry.allowedToSaveSearchInfo(searchInfo)
                                                        && PreferencesUtil.isAutofillSaveSearchInfoEnable(
                                                            this@GroupActivity
                                                        )
                                                    ) {
                                                        updateEntryWithRegisterInfo(
                                                            database = database,
                                                            entry = entry,
                                                            registerInfo = searchInfo!!.toRegisterInfo()
                                                        )
                                                    } else {
                                                        entrySelectedForSelection(entry)
                                                    }
                                                }

                                                TypeMode.PASSWORD -> {
                                                    entrySelectedForPasswordSelection(entry)
                                                }

                                                TypeMode.PASSKEY -> {
                                                    entrySelectedForPasskeySelection(entry)
                                                }
                                            }
                                            loadGroup()
                                        },
                                        registrationAction = { intentSenderMode, typeMode, registerInfo ->
                                            if (!database.isReadOnly) {
                                                entrySelectedForRegistration(
                                                    database = database,
                                                    entry = entry,
                                                    registerInfo = registerInfo,
                                                    typeMode = typeMode,
                                                    activityResultLauncher = if (intentSenderMode)
                                                        mCredentialActivityResultLauncher else null
                                                )
                                                loadGroup()
                                            } else
                                                finish()
                                        }
                                    )
                                }
                            }
                            is GroupViewModel.GroupEvent.EditGroup -> {
                                launchDialogForGroupUpdate(event.group)
                            }
                            is GroupViewModel.GroupEvent.EditEntry -> {
                                mDatabase?.let { database ->
                                    val entryId = event.entry.nodeId
                                    if (mDatabaseAllowUserVerification) {
                                        checkUserVerification(
                                            userVerificationViewModel = mUserVerificationViewModel,
                                            dataToVerify = UserVerificationData(
                                                actionType = UserVerificationActionType.EDIT_ENTRY,
                                                database = database,
                                                entryId = entryId
                                            )
                                        )
                                    } else {
                                        editEntry(database, entryId)
                                    }
                                }
                            }
                            is GroupViewModel.GroupEvent.ShowGroup -> {
                                launchDialogToShowGroupInfo(event.group)
                            }
                            is GroupViewModel.GroupEvent.CopyNodes -> {
                                loadGroup(clearSearch = true)
                            }
                            is GroupViewModel.GroupEvent.MoveNodes -> {
                                loadGroup(clearSearch = true)
                            }
                            is GroupViewModel.GroupEvent.DeleteNodes -> {
                                deleteNodes(event.deleteActionState.nodes, event.deleteActionState.isRecycleBin)
                            }
                            is GroupViewModel.GroupEvent.PasteNodes -> {
                                when (event.pasteActionState.pasteMode) {
                                    NodeActionMode.PASTE_FROM_COPY -> {
                                        copyNodes(
                                            newParentId = event.pasteActionState.parentId,
                                            nodesToCopy = event.pasteActionState.nodes
                                        )
                                    }
                                    NodeActionMode.PASTE_FROM_MOVE -> {
                                        moveNodes(
                                            newParentId = event.pasteActionState.parentId,
                                            nodesToMove = event.pasteActionState.nodes
                                        )
                                    }
                                    else -> {}
                                }
                            }
                            is GroupViewModel.GroupEvent.ScrollTo -> {
                                addNodeButtonView?.hideOrShowButtonOnScrollListener(event.dy)
                            }
                            is GroupViewModel.GroupEvent.HideKeyboard -> {
                                hideKeyboard()
                            }
                            is GroupViewModel.GroupEvent.ClearSearch -> {
                                hideKeyboard()
                                intent.removeAutoSearch()
                                if (Intent.ACTION_SEARCH == intent.action) {
                                    intent.action = Intent.ACTION_DEFAULT
                                    intent.removeExtra(SearchManager.QUERY)
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mSearchViewModel.searchActivated.collect { state ->
                        showSearch(state)
                    }
                }
                launch {
                    mGroupViewModel.nodeActionState.collect { state ->
                        if (state.isActive) {
                            if (activeActionMode == null) {
                                activeActionMode = toolbarAction?.startSupportActionMode(actionModeCallback)
                            }
                            activeActionMode?.invalidate()
                        } else {
                            activeActionMode?.finish()
                        }
                    }
                }
                launch {
                    mGroupEditViewModel.nodeEditEvents.collect { event ->
                        when (event) {
                            is NodeEditViewModel.NodeEditEvent.RequestIconSelection -> {
                                IconPickerActivity.launch(
                                    context = this@GroupActivity,
                                    previousIcon = event.icon,
                                    resultLauncher = mIconSelectionActivityResultLauncher
                                )
                            }
                            is NodeEditViewModel.NodeEditEvent.OnBackgroundColorSelected -> {
                                // TODO Group background color
                            }
                            is NodeEditViewModel.NodeEditEvent.OnForegroundColorSelected -> {
                                // TODO Group foreground color
                            }
                            is NodeEditViewModel.NodeEditEvent.RequestDateTimeSelection -> {
                                if (event.dateInstant.type == DateInstant.Type.TIME) {
                                    // Launch the time picker
                                    Builder().build().apply {
                                        addOnPositiveButtonClickListener {
                                            mGroupEditViewModel.selectTime(DataTime(this.hour, this.minute))
                                        }
                                        show(supportFragmentManager, "TimePickerFragment")
                                    }
                                } else {
                                    // Launch the date picker
                                    MaterialDatePicker.Builder.datePicker().build().apply {
                                        addOnPositiveButtonClickListener {
                                            mGroupEditViewModel.selectDate(datePickerToDataDate(it))
                                        }
                                        show(supportFragmentManager, "DatePickerFragment")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mMainCredentialDialogViewModel.mainCredentialEvent.collect { credentialState ->
                        when (credentialState) {
                            is MainCredentialDialogViewModel.MainCredentialEvent.OnMainCredentialEntered -> {
                                mergeDatabaseFrom(credentialState.databaseUri, credentialState.mainCredential)
                            }
                            is MainCredentialDialogViewModel.MainCredentialEvent.OnMainCredentialCanceled -> {}
                        }
                    }
                }
                launch {
                    mGroupEditViewModel.onCreateGroup.collect { groupInfo ->
                        mGroupViewModel.mainGroup?.nodeId?.let { parentId ->
                            createGroup(parentId, groupInfo)
                        }
                    }
                }
                launch {
                    mGroupEditViewModel.onUpdateGroup.collect { groupInfo ->
                        updateGroup(groupInfo)
                    }
                }
                launch {
                    mUserVerificationViewModel.onUserVerificationCanceled.collect { result ->
                        coordinatorLayout?.showError(result.error)
                    }
                }
                launch {
                    mUserVerificationViewModel.onUserVerificationSucceeded.collect { data ->
                        when (data.actionType) {
                            UserVerificationActionType.EDIT_ENTRY -> {
                                editEntry(data.database, data.entryId)
                            }
                            UserVerificationActionType.MERGE_FROM_DATABASE -> {
                                mExternalFileHelper?.openDocument()
                            }
                            UserVerificationActionType.SAVE_DATABASE_COPY_TO -> {
                                mExternalFileHelper?.createDocument(
                                    getString(R.string.database_file_name_default) +
                                            "_" +
                                            LocalDateTime.now().toString() +
                                            data.database?.defaultFileExtension
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Add listeners to the add buttons
        addNodeButtonView?.setAddGroupClickListener {
            launchDialogForGroupCreation()
        }
        addNodeButtonView?.setAddEntryClickListener {
            mDatabase?.let { database ->
                mGroupViewModel.mainGroup?.nodeId?.let { currentGroupId ->
                    EntrySelectionHelper.doSpecialAction(
                        intent = intent,
                        defaultAction = {
                            EntryEditActivity.launch(
                                activity = this@GroupActivity,
                                database = database,
                                registrationType = EntryEditActivity.RegistrationType.CREATE,
                                nodeId = currentGroupId,
                                activityResultLauncher = mEntryActivityResultLauncher
                            )
                        },
                        searchAction = {
                            // Search not used
                        },
                        selectionAction = { intentSenderMode, typeMode, searchInfo ->
                            EntryEditActivity.launchForSelection(
                                context = this@GroupActivity,
                                database = database,
                                typeMode = typeMode,
                                groupId = currentGroupId,
                                searchInfo = searchInfo,
                                activityResultLauncher = if (intentSenderMode)
                                    mCredentialActivityResultLauncher else null
                            )
                            onLaunchActivitySpecialMode()
                        },
                        registrationAction = { intentSenderMode, typeMode, registerInfo ->
                            EntryEditActivity.launchForRegistration(
                                context = this@GroupActivity,
                                database = database,
                                nodeId = currentGroupId,
                                registerInfo = registerInfo,
                                typeMode = typeMode,
                                registrationType = EntryEditActivity.RegistrationType.CREATE,
                                activityResultLauncher = if (intentSenderMode)
                                    mCredentialActivityResultLauncher else null
                            )
                            onLaunchActivitySpecialMode()
                        }
                    )
                }
            }
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return drawerLayout
    }

    private fun loadGroup(clearSearch: Boolean = false) {
        if (clearSearch)
            mSearchViewModel.deactivateSearch()
        mGroupViewModel.loadGroup()
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)

        mGroupViewModel.onDatabaseLoaded(database)

        mBreadcrumbAdapter?.apply {
            // Open group on breadcrumb click
            onItemClickListener = { group ->
                mGroupViewModel.onBreadcrumbClicked(group)
            }
            onLongItemClickListener = { group ->
                mGroupViewModel.onBreadcrumbLongClicked(group)
            }
        }
        breadcrumbListView?.apply {
            adapter = mBreadcrumbAdapter
        }

        mGroupEditViewModel.groupNamesNotAllowed = database.groupNamesNotAllowed

        loadGroup()

        // Update view
        mBreadcrumbAdapter?.iconDrawableFactory = database.iconDrawableFactory
        refreshDatabaseViews()
        invalidateOptionsMenu()
    }

    private fun refreshDatabaseViews() {
        mDatabase?.let {
            val databaseName = it.name.ifEmpty { getString(R.string.database) }
            databaseNavView?.setDatabaseName(databaseName)
            databaseNameView?.text = databaseName
            databaseNavView?.setDatabasePath(it.fileUri?.toString())
            databaseNavView?.setDatabaseVersion(it.version)
            val modified = it.dataModifiedSinceLastLoading
            databaseNavView?.setDatabaseModifiedSinceLastLoading(modified)
            databaseModifiedView?.isVisible = modified
            val customColor = it.customColor
            databaseNavView?.setDatabaseColor(customColor)
            if (customColor != null) {
                databaseColorView?.visibility = View.VISIBLE
                databaseColorView?.setColorFilter(
                    customColor,
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                databaseColorView?.visibility = View.GONE
            }
        }
    }

    override fun onDatabaseActionFinished(
        database: ContextualDatabase,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)
        when (actionTask) {
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                if (result.isSuccess) {
                    EntrySelectionHelper.doSpecialAction(
                        intent = intent,
                        defaultAction = {
                            // Standard not used after task
                        },
                        searchAction = {
                            // Search not used
                        },
                        selectionAction = { _, typeMode, _ ->
                            try {
                                result.data?.getNewEntry(database)?.let { entry ->
                                    when (typeMode) {
                                        TypeMode.DEFAULT -> {}
                                        TypeMode.MAGIKEYBOARD ->
                                            entrySelectedForSelection(entry)
                                        TypeMode.AUTOFILL ->
                                            entrySelectedForSelection(entry)
                                        TypeMode.PASSKEY ->
                                            entrySelectedForPasskeySelection(entry)
                                        TypeMode.PASSWORD ->
                                            entrySelectedForPasswordSelection(entry)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Unable to retrieve entry action for selection", e)
                            }
                        },
                        registrationAction = { _, _, _ ->
                            // Save not used
                        }
                    )
                }
            }
        }
        if (actionTask != ACTION_DATABASE_TOUCH_GROUP_TASK
            && actionTask != ACTION_DATABASE_TOUCH_ENTRY_TASK) {
            // Too many special cases to make specific additions or deletions,
            // rebuilt the list works well.
            loadGroup()
        }
        if (actionTask == ACTION_DATABASE_UPDATE_GROUP_TASK
            || actionTask == ACTION_DATABASE_UPDATE_ENTRY_TASK
            || actionTask == ACTION_DATABASE_MOVE_NODES_TASK
            || actionTask == ACTION_DATABASE_COPY_NODES_TASK) {
            coordinatorError?.showActionErrorIfNeeded(result)
            finishNodeAction()
        }
    }

    private fun showSearch(show: Boolean = true) {
        val searchFragment =
            supportFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAGE) as? SearchFragment
                ?: SearchFragment()

        // Attach fragment to content view
        if (!searchFragment.isAdded && show) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .add(
                    R.id.search_content_view,
                    searchFragment,
                    SEARCH_FRAGMENT_TAGE
                )
                .commit()
        } else if (searchFragment.isAdded && !show) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .remove(searchFragment)
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "setNewIntent: $intent")
        setIntent(intent)
        manageIntent(intent)
    }

    private fun manageIntent(intent: Intent?) {
        intent?.let {
            // Info search
            val searchInfo = if (it.retrieveAutoSearch()) it.retrieveSearchInfo() else null
            // External search
            val searchQuery = if (Intent.ACTION_SEARCH == it.action)
                it.getStringExtra(SearchManager.QUERY) else null
            mSearchViewModel.processSearchData(searchInfo, searchQuery)
            it.action = Intent.ACTION_DEFAULT
            it.removeExtra(SearchManager.QUERY)
        }
    }

    private fun entrySelectedForSelection(entry: EntryInfo) {
        mSearchViewModel.clearSearch()
        // Build response with the entry selected
        this.buildSpecialModeResponseAndSetResult(entry)
        onValidateSpecialMode()
    }

    private fun entrySelectedForPasswordSelection(entry: EntryInfo) {
        mSearchViewModel.clearSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            buildPasswordResponseAndSetResult(entryInfo = entry)
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForPasskeySelection(entry: EntryInfo) {
        mSearchViewModel.clearSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            buildPasskeyResponseAndSetResult(entryInfo = entry)
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForRegistration(
        database: ContextualDatabase,
        entry: EntryInfo,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        registerInfo: RegisterInfo?,
        typeMode: TypeMode
    ) {
        mSearchViewModel.clearSearch()
        // Registration to update the entry
        EntryEditActivity.launchForRegistration(
            context = this@GroupActivity,
            database = database,
            activityResultLauncher = activityResultLauncher,
            nodeId = entry.nodeId,
            registerInfo = registerInfo,
            typeMode = typeMode,
            registrationType = EntryEditActivity.RegistrationType.UPDATE
        )
        onLaunchActivitySpecialMode()
    }

    private fun updateEntryWithRegisterInfo(
        database: ContextualDatabase,
        entry: EntryInfo,
        registerInfo: RegisterInfo
    ) {
        database.saveRegisterInfoIn(entry, registerInfo)
        updateEntry(entry)
    }

    private fun editEntry(database: ContextualDatabase?, entryId: EntryId?) {
        database?.let {
            entryId?.let {
                EntryEditActivity.launch(
                    activity = this@GroupActivity,
                    database = database,
                    registrationType = EntryEditActivity.RegistrationType.UPDATE,
                    nodeId = entryId,
                    activityResultLauncher = mEntryActivityResultLauncher
                )
            }
        }
    }

    private fun finishNodeAction() {
        mGroupViewModel.finishNodeAction()
    }

    private fun launchDialogToShowGroupInfo(group: GroupInfo) {
        GroupDialogFragment.launch(group)
            .show(supportFragmentManager, GroupDialogFragment.TAG_SHOW_GROUP)
    }

    private fun launchDialogForGroupCreation() {
        GroupEditDialogFragment.create(GroupInfo().apply {
            // Init notes if available
            if (mDatabase?.allowAddNoteInEachGroup == true) { notes = "" }
        }).show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogForGroupUpdate(group: GroupInfo) {
        GroupEditDialogFragment.update(group)
            .show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogToAskMainCredential(uri: Uri?) {
        MainCredentialDialogFragment.getInstance(uri)
            .show(supportFragmentManager, MainCredentialDialogFragment.TAG_ASK_MAIN_CREDENTIAL)
    }

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // Padding if lock button visible
        toolbarAction?.updateButtonPaddingStart()

        loadGroup()
    }

    private fun prepareDatabaseNavMenu() {
        // hide or show nav menu
        databaseNavView?.apply {
            //  depending on current mode
            val modeCondition = mSpecialMode == SpecialMode.DEFAULT
            menu.findItem(R.id.menu_app_settings)?.isVisible = modeCondition
            menu.findItem(R.id.menu_merge_from)?.isVisible = mGroupViewModel.mergeFromDatabaseActionAllowed() && modeCondition
            menu.findItem(R.id.menu_save_copy_to)?.isVisible = mGroupViewModel.copyToDatabaseActionAllowed() && modeCondition
            menu.findItem(R.id.menu_about)?.isVisible = modeCondition
            menu.findItem(R.id.menu_contribute)?.isVisible = modeCondition
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        if (mGroupViewModel.databaseActionsAllowed()) {
            val inflater = menuInflater
            inflater.inflate(R.menu.search, menu)
            inflater.inflate(R.menu.tree, menu)
            inflater.inflate(R.menu.database, menu)
            if (!mGroupViewModel.saveDatabaseActionAllowed()) {
                menu.findItem(R.id.menu_save_database)?.isVisible = false
            }
            if (!mGroupViewModel.mergeDatabaseActionAllowed()
                || mSpecialMode != SpecialMode.DEFAULT
            ) {
                menu.findItem(R.id.menu_merge_database)?.isVisible = false
            }
            if (!mGroupViewModel.reloadDatabaseActionAllowed()
                || mSpecialMode != SpecialMode.DEFAULT
            ) {
                menu.findItem(R.id.menu_reload_database)?.isVisible = false
            }
            // Menu for recycle bin
            if (mGroupViewModel.recycleBinActionsAllowed()) {
                inflater.inflate(R.menu.recycle_bin, menu)
            }

            prepareDatabaseNavMenu()

            // Launch education screen
            Handler(Looper.getMainLooper()).post {
                performedNextEducation(menu)
            }
        }

        return true
    }

    private fun performedNextEducation(menu: Menu) {

        // If no node, show education to add new one
        val addNodeButtonEducationPerformed = !mGroupViewModel.actionNodeInProgress()
                && addNodeButtonView?.addButtonView != null
                && addNodeButtonView!!.isEnable
                && mGroupActivityEducation.checkAndPerformedAddNodeButtonEducation(
            addNodeButtonView?.addButtonView!!,
            {
                addNodeButtonView?.openButtonIfClose()
            },
            {
                performedNextEducation(menu)
            }
        )
        if (!addNodeButtonEducationPerformed) {

            val searchMenuEducationPerformed = toolbar != null
                    && toolbar!!.findViewById<View>(R.id.menu_search) != null
                    && mGroupActivityEducation.checkAndPerformedSearchMenuEducation(
                toolbar!!.findViewById(R.id.menu_search),
                {
                    menu.findItem(R.id.menu_search).expandActionView()
                },
                {
                    performedNextEducation(menu)
                })

            if (!searchMenuEducationPerformed) {

                val sortMenuEducationPerformed = toolbar != null
                        && toolbar!!.findViewById<View>(R.id.menu_sort) != null
                        && mGroupActivityEducation.checkAndPerformedSortMenuEducation(
                    toolbar!!.findViewById(R.id.menu_sort),
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_sort))
                    },
                    {
                        performedNextEducation(menu)
                    })

                if (!sortMenuEducationPerformed) {
                    // lockMenuEducationPerformed
                    val lockButtonView = findViewById<View>(R.id.lock_button)
                    lockButtonView != null
                            && mGroupActivityEducation.checkAndPerformedLockMenuEducation(
                        lockButtonView,
                        {
                            lockAndExit()
                        },
                        {
                            performedNextEducation(menu)
                        })
                }
            }
        }
    }

    override fun hideHomeButtonIfModeIsNotDefault(): Boolean {
        return false
    }

    private fun requestSort() {
        SortDialogFragment.getInstance()
            .show(supportFragmentManager, "sortDialog")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout?.openDrawer(GravityCompat.START)
                return true
            }
            R.id.menu_search -> {
                mSearchViewModel.activateSearch()
                return true
            }
            R.id.menu_sort -> {
                requestSort()
                return true
            }
            R.id.menu_save_database -> {
                saveDatabase()
                return true
            }
            R.id.menu_merge_database -> {
                mergeDatabase()
                return true
            }
            R.id.menu_reload_database -> {
                reloadDatabase()
                return true
            }
            R.id.menu_empty_recycle_bin -> {
                mGroupViewModel.emptyRecycleBin()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCancelSpecialMode() {
        super.onCancelSpecialMode()
        loadGroup(clearSearch = true)
    }

    override fun startActivity(intent: Intent) {
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            // manually launch the same search activity
            val searchIntent = getIntent().apply {
                // add query to the Intent Extras
                action = Intent.ACTION_SEARCH
                putExtra(SearchManager.QUERY, intent.getStringExtra(SearchManager.QUERY))
            }
            setIntent(searchIntent)
            onNewIntent(searchIntent)
        } else {
            super.startActivity(intent)
        }
    }

    override fun onDatabaseBackPressed() {
        if (mGroupViewModel.nodeActionState.value.nodeActionMode != null) {
            finishNodeAction()
        } else {
            // Normal way when we are not in root
            when {
                Intent.ACTION_SEARCH == intent.action -> {
                    // Load the main group after remove search
                    loadGroup(clearSearch = true)
                }
                mSearchViewModel.isSearchActivated -> {
                    mSearchViewModel.deactivateSearch()
                }
                mGroupViewModel.previousGroupExists() -> {
                    // Load the previous group
                    mGroupViewModel.loadPreviousGroup()
                }
                mGroupViewModel.isCurrentGroupIsRoot -> {
                    // In root, lock if needed
                    mSearchViewModel.clearSearch()
                    intent.removeModes()
                    intent.removeInfo()
                    if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
                        lockAndExit()
                    } else {
                        backToTheAppCaller()
                    }
                }
                else -> {
                    super.onRegularBackPressed()
                }
            }
        }
    }

    companion object {

        private val TAG = GroupActivity::class.java.name

        private const val GROUP_FRAGMENT_TAG = "GROUP_FRAGMENT_TAG"
        private const val SEARCH_FRAGMENT_TAGE = "SEARCH_FRAGMENT_TAGE"
        private const val AUTO_SEARCH_KEY = "AUTO_SEARCH_KEY"

        fun Intent.retrieveAutoSearch(): Boolean {
            return this.getBooleanExtra(AUTO_SEARCH_KEY, false)
        }

        fun Intent.addAutoSearch(autoSearch: Boolean) {
            this.putExtra(AUTO_SEARCH_KEY, autoSearch)
        }

        fun Intent.removeAutoSearch() {
            this.removeExtra(AUTO_SEARCH_KEY)
        }

        private fun buildIntent(
            context: Context,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            intentBuildLauncher.invoke(
                Intent(context, GroupActivity::class.java)
            )
        }

        private fun checkTimeAndBuildIntent(
            context: Context,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                buildIntent(context, intentBuildLauncher)
            }
        }

        /*
         * -------------------------
         * 		Standard Launch
         * -------------------------
         */
        fun launch(
            context: Context,
            database: ContextualDatabase,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context) { intent ->
                    intent.addAutoSearch(autoSearch)
                    context.startActivity(intent)
                }
            }
        }

        /*
         * -------------------------
         * 		Search Launch
         * -------------------------
         */
        fun launchForSearch(
            context: Context,
            database: ContextualDatabase,
            searchInfo: SearchInfo,
            autoSearch: Boolean = false
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context) { intent ->
                    intent.addAutoSearch(autoSearch)
                    intent.addSearchInfo(searchInfo)
                    context.startActivity(intent)
                }
            }
        }

        /*
         * -------------------------
         * 		Selection Launch
         * -------------------------
         */
        fun launchForSelection(
            context: Context,
            database: ContextualDatabase,
            typeMode: TypeMode,
            searchInfo: SearchInfo? = null,
            autoSearch: Boolean = false,
            activityResultLauncher: ActivityResultLauncher<Intent>?,
        ) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context) { intent ->
                    intent.addAutoSearch(autoSearch)
                    EntrySelectionHelper.startActivityForSelectionModeResult(
                        context = context,
                        intent = intent,
                        typeMode = typeMode,
                        searchInfo = searchInfo,
                        activityResultLauncher = activityResultLauncher
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Registration Launch
         * -------------------------
         */
        fun launchForRegistration(
            context: Context,
            database: ContextualDatabase,
            typeMode: TypeMode,
            registerInfo: RegisterInfo? = null,
            activityResultLauncher: ActivityResultLauncher<Intent>?,
        ) {
            if (database.loaded && !database.isReadOnly) {
                checkTimeAndBuildIntent(context) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, false)
                    EntrySelectionHelper.startActivityForRegistrationModeResult(
                        context,
                        activityResultLauncher,
                        intent,
                        registerInfo,
                        typeMode
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Global Launch
         * -------------------------
         */
        fun launch(
            activity: Activity,
            database: ContextualDatabase,
            onValidateSpecialMode: () -> Unit,
            onCancelSpecialMode: () -> Unit,
            onLaunchActivitySpecialMode: () -> Unit,
            activityResultLauncher: ActivityResultLauncher<Intent>?
        ) {
            EntrySelectionHelper.doSpecialAction(
                intent = activity.intent,
                defaultAction = {
                    // Default action
                    launch(
                        activity,
                        database,
                        true
                    )
                },
                searchAction = { searchInfo ->
                    // Search action
                    if (database.loaded) {
                        launchForSearch(activity,
                            database,
                            searchInfo,
                            true)
                        onLaunchActivitySpecialMode()
                    } else {
                        // Simply close if database not opened
                        onCancelSpecialMode()
                    }
                },
                selectionAction = { intentSenderMode, typeMode, searchInfo ->
                    SearchHelper.checkAutoSearchInfo(
                        context = activity,
                        database = database,
                        searchInfo = searchInfo,
                        onItemsFound = { _, items ->
                            when (typeMode) {
                                TypeMode.DEFAULT -> {}
                                TypeMode.MAGIKEYBOARD -> {
                                    activity.buildSpecialModeResponseAndSetResult(items)
                                    onValidateSpecialMode()
                                }
                                TypeMode.AUTOFILL -> {
                                    // Response is build
                                    activity.buildSpecialModeResponseAndSetResult(items)
                                    onValidateSpecialMode()
                                }
                                TypeMode.PASSWORD -> {
                                    EntrySelectionHelper.performSelection(
                                        items = items,
                                        actionPopulateCredentialProvider = { entryInfo ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                activity.buildPasswordResponseAndSetResult(entryInfo)
                                            }
                                            onValidateSpecialMode()
                                        },
                                        actionEntrySelection = {
                                            launchForSelection(
                                                context = activity,
                                                database = database,
                                                typeMode = TypeMode.PASSWORD,
                                                searchInfo = searchInfo,
                                                activityResultLauncher = activityResultLauncher,
                                                autoSearch = true
                                            )
                                            onLaunchActivitySpecialMode()
                                        }
                                    )
                                }
                                TypeMode.PASSKEY -> {
                                    // Response is build
                                    EntrySelectionHelper.performSelection(
                                        items = items,
                                        actionPopulateCredentialProvider = { entryInfo ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                activity.buildPasskeyResponseAndSetResult(entryInfo)
                                            }
                                            onValidateSpecialMode()
                                        },
                                        actionEntrySelection = {
                                            launchForSelection(
                                                context = activity,
                                                database = database,
                                                typeMode = TypeMode.PASSKEY,
                                                searchInfo = searchInfo,
                                                activityResultLauncher = activityResultLauncher,
                                                autoSearch = true
                                            )
                                            onLaunchActivitySpecialMode()
                                        }
                                    )
                                }
                            }
                        },
                        onItemNotFound = {
                            // Here no search info found, disable auto search
                            launchForSelection(
                                context = activity,
                                database = database,
                                typeMode = typeMode,
                                searchInfo = searchInfo,
                                autoSearch = false,
                                activityResultLauncher = if (intentSenderMode)
                                    activityResultLauncher else null
                            )
                            onLaunchActivitySpecialMode()
                        },
                        onDatabaseClosed = {
                            // Simply close if database not opened, normally not happened
                            onCancelSpecialMode()
                        }
                    )
                },
                registrationAction = { intentSenderMode, typeMode, registerInfo ->
                    // Save info
                    if (database.loaded) {
                        if (!database.isReadOnly) {
                            launchForRegistration(
                                context = activity,
                                database = database,
                                registerInfo = registerInfo,
                                typeMode = typeMode,
                                activityResultLauncher = if (intentSenderMode)
                                    activityResultLauncher else null
                            )
                            onLaunchActivitySpecialMode()
                        } else {
                            activity.toastError(RegisterInReadOnlyDatabaseException())
                            onCancelSpecialMode()
                        }
                    } else {
                        onCancelSpecialMode()
                    }
                }
            )
        }
    }
}
