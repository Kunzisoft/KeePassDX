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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GroupDialogFragment
import com.kunzisoft.keepass.activities.dialogs.GroupEditDialogFragment
import com.kunzisoft.keepass.activities.dialogs.MainCredentialDialogFragment
import com.kunzisoft.keepass.activities.dialogs.SortDialogFragment
import com.kunzisoft.keepass.activities.fragments.GroupFragment
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
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.buildPasskeyResponseAndSetResult
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.MainCredential
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.model.DataTime
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getNewEntry
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard
import com.kunzisoft.keepass.utils.TimeUtil.datePickerToDataDate
import com.kunzisoft.keepass.utils.UriUtil.openUrl
import com.kunzisoft.keepass.utils.getParcelableCompat
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.view.AddNodeButtonView
import com.kunzisoft.keepass.view.NavigationDatabaseView
import com.kunzisoft.keepass.view.SearchFiltersView
import com.kunzisoft.keepass.view.ToolbarAction
import com.kunzisoft.keepass.view.WindowInsetPosition
import com.kunzisoft.keepass.view.applyWindowInsets
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.setTransparentNavigationBar
import com.kunzisoft.keepass.view.showActionErrorIfNeeded
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.view.updateLockPaddingStart
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import org.joda.time.LocalDateTime
import java.util.EnumSet


class GroupActivity : DatabaseLockActivity(),
        GroupFragment.NodeClickListener,
        GroupFragment.NodesActionMenuListener,
        GroupFragment.OnScrollListener,
        GroupFragment.GroupRefreshedListener,
        SortDialogFragment.SortSelectionListener,
        MainCredentialDialogFragment.AskMainCredentialDialogListener {

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
    private var searchView: SearchView? = null
    private var searchFiltersView: SearchFiltersView? = null
    private var toolbarAction: ToolbarAction? = null
    private var numberChildrenView: TextView? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var breadcrumbListView: RecyclerView? = null
    private var loadingView: ProgressBar? = null

    private val mGroupViewModel: GroupViewModel by viewModels()
    private val mGroupEditViewModel: GroupEditViewModel by viewModels()

    private val mGroupActivityEducation = GroupActivityEducation(this)

    private var mBreadcrumbAdapter: BreadcrumbAdapter? = null

    private var mGroupFragment: GroupFragment? = null
    private var mRecyclingBinEnabled = false
    private var mRecyclingBinIsCurrentGroup = false
    private var mRequestStartupSearch = true

    private var actionNodeMode: ActionMode? = null

    // Manage merge
    private var mExternalFileHelper: ExternalFileHelper? = null

    // Manage group
    private var mSearchState: SearchState? = null
    private var mAutoSearch: Boolean = false // To mainly manage keyboard
    private var mTempSearchInfo: Boolean = false // To manage temp search
    private var mMainGroupState: GroupState? = null // Group state, not a search
    private var mRootGroup: Group? = null // Root group in the tree
    private var mMainGroup: Group? = null // Main group currently in memory
    private var mCurrentGroup: Group? = null // Group currently visible (search or main group)
    private var mPreviousGroupsIds = mutableListOf<GroupState>()
    private var mOldGroupToUpdate: Group? = null

    private var mLockSearchListeners = false
    private val mOnSearchQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            onQueryTextChange(query)
            // Collapse the search filters
            searchFiltersView?.closeAdvancedFilters()
            // Close the keyboard
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText != null && !mLockSearchListeners) {
                mSearchState?.let { searchState ->
                    searchState.searchParameters.searchQuery = newText
                    loadSearchGroup(searchState)
                }
            }
            return true
        }
    }
    private val mOnSearchFiltersChangeListener = object : ((SearchParameters) -> Unit) {
        override fun invoke(searchParameters: SearchParameters) {
            mSearchState?.let { searchState ->
                searchParameters.searchQuery = searchState.searchParameters.searchQuery
                searchState.searchParameters = searchParameters
                loadSearchGroup(searchState)
            }
        }
    }
    private val mOnSearchActionExpandListener = object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
            searchFiltersView?.visibility = View.VISIBLE
            searchFiltersView?.showSearchExpandButton(!mTempSearchInfo)
            searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)
            searchFiltersView?.onParametersChangeListener = mOnSearchFiltersChangeListener

            addSearch()
            return true
        }

        override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
            searchFiltersView?.onParametersChangeListener = null
            searchView?.setOnQueryTextListener(null)
            searchFiltersView?.visibility = View.GONE

            removeSearch()
            loadGroup()
            return true
        }
    }
    private val mOnSearchTextFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
        if (!mAutoSearch
            && hasFocus
            && PreferencesUtil.isKeyboardPreviousSearchEnable(this@GroupActivity)) {
            // Change to the previous keyboard and show it
            sendBroadcast(Intent(BACK_PREVIOUS_KEYBOARD_ACTION))
            view.showKeyboard()
        }
    }

    private val mEntryActivityResultLauncher = EntryEditActivity.registerForEntryResult(this) { entryId ->
        entryId?.let {
            // Simply refresh the list when entry is updated
            loadGroup()
        } ?: Log.e(this.javaClass.name, "Entry cannot be retrieved in Activity Result")
    }

    private fun addSearch() {
        finishNodeAction()
        if (mSearchState == null) {
            mSearchState = SearchState(searchFiltersView?.searchParameters
                ?: PreferencesUtil.getDefaultSearchParameters(this), 0)
        }
    }

    private fun removeSearch() {
        mSearchState = null
        mTempSearchInfo = false
        intent.removeExtra(AUTO_SEARCH_KEY)
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
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
        searchFiltersView = findViewById(R.id.search_filters)
        breadcrumbListView = findViewById(R.id.breadcrumb_list)
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

        // Manage 'merge from" and "save to"
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
                        mExternalFileHelper?.openDocument()
                    }
                    R.id.menu_save_copy_to -> {
                        mExternalFileHelper?.createDocument(
                            getString(R.string.database_file_name_default) +
                                    "_" +
                                    LocalDateTime.now().toString() +
                                    mDatabase?.defaultFileExtension)
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

        searchFiltersView?.closeAdvancedFilters()

        // Retrieve group if defined at launch
        manageIntent(intent)

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(REQUEST_STARTUP_SEARCH_KEY)) {
                mRequestStartupSearch = savedInstanceState.getBoolean(REQUEST_STARTUP_SEARCH_KEY)
                savedInstanceState.remove(REQUEST_STARTUP_SEARCH_KEY)
            }
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY)) {
                mOldGroupToUpdate = savedInstanceState.getParcelableCompat(OLD_GROUP_TO_UPDATE_KEY)
                savedInstanceState.remove(OLD_GROUP_TO_UPDATE_KEY)
            }
        }

        // Retrieve previous groups
        if (savedInstanceState != null && savedInstanceState.containsKey(PREVIOUS_GROUPS_IDS_KEY)) {
            try {
                mPreviousGroupsIds = savedInstanceState.getParcelableList(PREVIOUS_GROUPS_IDS_KEY)
                    ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Unable to retrieve previous groups", e)
            }
            savedInstanceState.remove(PREVIOUS_GROUPS_IDS_KEY)
        }

        // Initialize the fragment with the list
        mGroupFragment =
            supportFragmentManager.findFragmentByTag(GROUP_FRAGMENT_TAG) as GroupFragment?
        if (mGroupFragment == null)
            mGroupFragment = GroupFragment()

        // Attach fragment to content view
        supportFragmentManager.beginTransaction().replace(
            R.id.nodes_list_fragment_container,
            mGroupFragment!!,
            GROUP_FRAGMENT_TAG
        ).commit()

        // Observe main group
        mGroupViewModel.mainGroup.observe(this) {
            val mainGroup = it.group
            mMainGroup = mainGroup
            mRecyclingBinIsCurrentGroup = it.isRecycleBin
            // Save group state
            mMainGroupState = GroupState(mainGroup.nodeId, it.showFromPosition)
            // Update last access time.
            mainGroup.touch(modified = false, touchParents = false)
        }

        // Observe current group (main or search group visible)
        mGroupViewModel.group.observe(this) {
            val currentGroup = it.group
            mCurrentGroup = currentGroup
            if (currentGroup.isVirtual) {
                mSearchState = SearchState(
                    it.searchParameters,
                    it.showFromPosition
                )
            }
            // Main and search groups in activity are managed with another variables
            // to keep values during orientation

            loadingView?.hideByFading()
        }

        mGroupViewModel.firstPositionVisible.observe(this) { firstPositionVisible ->
            mSearchState?.firstVisibleItem = firstPositionVisible
            mMainGroupState?.firstVisibleItem = firstPositionVisible
        }

        mGroupEditViewModel.requestIconSelection.observe(this) { iconImage ->
            IconPickerActivity.launch(this@GroupActivity, iconImage, mIconSelectionActivityResultLauncher)
        }

        mGroupEditViewModel.requestDateTimeSelection.observe(this) { dateInstant ->
            if (dateInstant.type == DateInstant.Type.TIME) {
                // Launch the time picker
                MaterialTimePicker.Builder().build().apply {
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

        mGroupEditViewModel.onGroupCreated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                mMainGroup?.let { parentGroup ->
                    createGroup(parentGroup, groupInfo)
                }
            }
        }

        mGroupEditViewModel.onGroupUpdated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                groupInfo.id?.let { groupId ->
                    mDatabase?.getGroupById(NodeIdUUID(groupId))?.let { oldGroupToUpdate ->
                        updateGroup(oldGroupToUpdate, groupInfo)
                    }
                }
            }
        }

        // Add listeners to the add buttons
        addNodeButtonView?.setAddGroupClickListener {
            mMainGroup?.let { currentGroup ->
                launchDialogForGroupCreation(currentGroup)
            }
        }
        addNodeButtonView?.setAddEntryClickListener {
            mDatabase?.let { database ->
                mMainGroup?.let { currentGroup ->
                    EntrySelectionHelper.doSpecialAction(
                        intent = intent,
                        defaultAction = {
                            mMainGroup?.nodeId?.let { currentParentGroupId ->
                                EntryEditActivity.launch(
                                    activity = this@GroupActivity,
                                    database = database,
                                    registrationType = EntryEditActivity.RegistrationType.CREATE,
                                    nodeId = currentParentGroupId,
                                    activityResultLauncher = mEntryActivityResultLauncher
                                )
                            }
                        },
                        searchAction = {
                            // Search not used
                        },
                        selectionAction = { intentSenderMode, typeMode, searchInfo ->
                            EntryEditActivity.launchForSelection(
                                context = this@GroupActivity,
                                database = database,
                                typeMode = typeMode,
                                groupId = currentGroup.nodeId,
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
                                nodeId = currentGroup.nodeId,
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

    private fun loadMainGroup(groupState: GroupState) {
        mGroupViewModel.loadMainGroup(mDatabase,
            groupState.groupId,
            groupState.firstVisibleItem)
    }

    private fun loadSearchGroup(searchState: SearchState) {
        mGroupViewModel.loadSearchGroup(mDatabase,
            searchState.searchParameters,
            mMainGroupState?.groupId,
            searchState.firstVisibleItem)
    }

    private fun loadGroup() {
        val searchState = mSearchState
        val currentGroupState = mMainGroupState
        when {
            searchState != null -> {
                finishNodeAction()
                loadSearchGroup(searchState)
            }
            currentGroupState != null -> {
                loadMainGroup(currentGroupState)
            }
            else -> {
                loadMainGroup(GroupState(null, 0))
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)

        mBreadcrumbAdapter = BreadcrumbAdapter(this, database).apply {
            // Open group on breadcrumb click
            onItemClickListener = { node, _ ->
                // If last item & not a virtual root group
                val currentGroup = mMainGroup
                if (currentGroup != null && node == currentGroup
                    && (currentGroup != mDatabase?.rootGroup
                            || mDatabase?.rootGroupIsVirtual == false)
                ) {
                    finishNodeAction()
                    launchDialogToShowGroupInfo(currentGroup)
                } else {
                    if (mGroupFragment?.nodeActionSelectionMode == true) {
                        finishNodeAction()
                    }
                    mDatabase?.let { database ->
                        onNodeClick(database, node)
                    }
                }
            }
            onLongItemClickListener = { node, position ->
                val currentGroup = mMainGroup
                if (currentGroup != null && node == currentGroup
                    && (currentGroup != mDatabase?.rootGroup
                            || mDatabase?.rootGroupIsVirtual == false)
                ) {
                    finishNodeAction()
                    launchDialogForGroupUpdate(currentGroup)
                } else {
                    onItemClickListener?.invoke(node, position)
                }
            }
        }
        breadcrumbListView?.apply {
            adapter = mBreadcrumbAdapter
        }

        mGroupEditViewModel.setGroupNamesNotAllowed(database.groupNamesNotAllowed)

        mRecyclingBinEnabled = !mDatabaseReadOnly
                && database.isRecycleBinEnabled == true

        mRootGroup = database.rootGroup
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

        var entry: Entry? = null
        try {
            entry = result.data?.getNewEntry(database)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve entry action for selection", e)
        }

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
                        selectionAction = { intentSenderMode, typeMode, searchInfo ->
                            when (typeMode) {
                                TypeMode.DEFAULT -> {}
                                TypeMode.MAGIKEYBOARD -> entry?.let {
                                    entrySelectedForKeyboardSelection(database, it)
                                }
                                TypeMode.PASSKEY -> entry?.let {
                                    entrySelectedForPasskeySelection(database, it)
                                }
                                TypeMode.AUTOFILL -> entry?.let {
                                    entrySelectedForAutofillSelection(database, it)
                                }
                            }
                        },
                        registrationAction = { intentSenderMode, typeMode, searchInfo ->
                            // Save not used
                        }
                    )
                }
            }
        }

        coordinatorError?.showActionErrorIfNeeded(result)

        // Reload the group
        loadGroup()
        finishNodeAction()
    }

    private fun manageIntent(intent: Intent?) {
        intent?.let {
            if (intent.extras?.containsKey(GROUP_STATE_KEY) == true) {
                mMainGroupState = intent.getParcelableExtraCompat(GROUP_STATE_KEY)
                intent.removeExtra(GROUP_STATE_KEY)
            }
            // To get the form filling search as temp search
            val searchInfo: SearchInfo? = intent.retrieveSearchInfo()
            val autoSearch = intent.getBooleanExtra(AUTO_SEARCH_KEY, false)
            // Get search query
            if (searchInfo != null && autoSearch) {
                mAutoSearch = true
                mTempSearchInfo = true
                searchInfo.getSearchParametersFromSearchInfo(this) {
                    mSearchState = SearchState(
                        searchParameters = it,
                        firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
                    )
                }
            } else if (intent.action == Intent.ACTION_SEARCH) {
                mAutoSearch = true
                mSearchState = SearchState(
                    searchParameters = PreferencesUtil.getDefaultSearchParameters(this).apply {
                        searchQuery = intent.getStringExtra(SearchManager.QUERY)
                            ?.trim { it <= ' ' } ?: ""
                    },
                    firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
                )
            } else if (mRequestStartupSearch
                && PreferencesUtil.automaticallyFocusSearch(this@GroupActivity)) {
                // Expand the search view if defined in settings
                // To request search only one time
                mRequestStartupSearch = false
                addSearch()
            }
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "setNewIntent: $intent")
        setIntent(intent)
        manageIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableList(PREVIOUS_GROUPS_IDS_KEY, mPreviousGroupsIds)
        mOldGroupToUpdate?.let {
            outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, it)
        }
        outState.putBoolean(REQUEST_STARTUP_SEARCH_KEY, mRequestStartupSearch)
        super.onSaveInstanceState(outState)
    }

    override fun onGroupRefreshed() {
        val group = mCurrentGroup
        // Assign title
        if (group?.isVirtual == true) {
            searchFiltersView?.setNumbers(group.numberOfChildEntries)
            searchFiltersView?.setCurrentGroupText(mMainGroup?.title ?: getString(R.string.search))
            searchFiltersView?.availableOther(mDatabase?.allowEntryCustomFields() ?: false)
            searchFiltersView?.availableApplicationIds(mDatabase?.allowEntryCustomFields() ?: false)
            searchFiltersView?.availableTags(mDatabase?.allowTags() ?: false)
            searchFiltersView?.enableTags(mDatabase?.tagPool?.isNotEmpty() ?: false)
            searchFiltersView?.availableSearchableGroup(mDatabase?.allowCustomSearchableGroup() ?: false)
            searchFiltersView?.availableTemplates(mDatabase?.allowTemplatesGroup ?: false)
            searchFiltersView?.enableTemplates(mDatabase?.templatesGroup != null)
        } else {
            // Add breadcrumb
            setBreadcrumbNode(group)
            refreshDatabaseViews()
            invalidateOptionsMenu()
        }
        initAddButton(group)
    }

    private fun setBreadcrumbNode(group: Group?) {
        mBreadcrumbAdapter?.apply {
            setNode(group)
            breadcrumbListView?.scrollToPosition(itemCount -1)
        }
    }

    private fun initAddButton(group: Group?) {
        addNodeButtonView?.apply {
            closeButtonIfOpen()
            // To enable add button
            val addGroupEnabled = !mDatabaseReadOnly && group?.isVirtual != true
            var addEntryEnabled = !mDatabaseReadOnly && group?.isVirtual != true
            group?.let {
                if (!it.allowAddEntryIfIsRoot)
                    addEntryEnabled = it != mRootGroup && addEntryEnabled
            }
            enableAddGroup(addGroupEnabled)
            enableAddEntry(addEntryEnabled)
            if (group?.isVirtual == true)
                hideButton()
            else if (actionNodeMode == null)
                showButton()
        }
    }

    override fun onScrolled(dy: Int) {
        if (actionNodeMode == null)
            addNodeButtonView?.hideOrShowButtonOnScrollListener(dy)
    }

    override fun onNodeClick(
        database: ContextualDatabase,
        node: Node
    ) {
        when (node.type) {
            Type.GROUP -> try {
                val group = node as Group
                // Save the last not virtual group and it's position
                if (mCurrentGroup?.isVirtual == false) {
                    mMainGroupState?.let {
                        mPreviousGroupsIds.add(it)
                    }
                }
                // Open child group
                loadMainGroup(GroupState(group.nodeId, 0))
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Group", e)
            }

            Type.ENTRY -> try {
                val entryVersioned = node as Entry
                EntrySelectionHelper.doSpecialAction(
                    intent = intent,
                    defaultAction = {
                        EntryActivity.launch(
                            activity = this@GroupActivity,
                            database = database,
                            entryId = entryVersioned.nodeId,
                            activityResultLauncher = mEntryActivityResultLauncher
                        )
                        // Do not reload group here
                    },
                    searchAction = {
                        // Nothing here, a search is simply performed
                    },
                    selectionAction = { intentSenderMode, typeMode, searchInfo ->
                        when (typeMode) {
                            TypeMode.DEFAULT -> {}
                            TypeMode.MAGIKEYBOARD -> {
                                if (!database.isReadOnly
                                    && searchInfo != null
                                    && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@GroupActivity)
                                    && entryVersioned.containsSearchInfo(database, searchInfo).not()
                                ) {
                                    updateEntryWithRegisterInfo(
                                        database,
                                        entryVersioned,
                                        searchInfo.toRegisterInfo()
                                    )
                                } else {
                                    entrySelectedForKeyboardSelection(database, entryVersioned)
                                }
                            }
                            TypeMode.PASSKEY -> {
                                entrySelectedForPasskeySelection(database, entryVersioned)
                            }
                            TypeMode.AUTOFILL -> {
                                if (!database.isReadOnly
                                    && searchInfo != null
                                    && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@GroupActivity)
                                    && entryVersioned.containsSearchInfo(database, searchInfo).not()
                                ) {
                                    updateEntryWithRegisterInfo(
                                        database,
                                        entryVersioned,
                                        searchInfo.toRegisterInfo()
                                    )
                                } else {
                                    entrySelectedForAutofillSelection(database, entryVersioned)
                                }
                            }
                        }
                        loadGroup()
                    },
                    registrationAction = { intentSenderMode, typeMode, registerInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForRegistration(
                                database = database,
                                entry = entryVersioned,
                                registerInfo = registerInfo,
                                typeMode = typeMode,
                                activityResultLauncher = if (intentSenderMode)
                                    mCredentialActivityResultLauncher else null
                            )
                            loadGroup()
                        } else
                            finish()
                    })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry", e)
            }
        }
    }

    private fun entrySelectedForKeyboardSelection(database: ContextualDatabase, entry: Entry) {
        removeSearch()
        // Build response with the entry selected
        this.buildSpecialModeResponseAndSetResult(entry.getEntryInfo(database))
        onValidateSpecialMode()
    }

    private fun entrySelectedForAutofillSelection(database: ContextualDatabase, entry: Entry) {
        removeSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.buildSpecialModeResponseAndSetResult(entry.getEntryInfo(database))
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForPasskeySelection(database: ContextualDatabase, entry: Entry) {
        removeSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            buildPasskeyResponseAndSetResult(
                entryInfo = entry.getEntryInfo(database)
            )
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForRegistration(
        database: ContextualDatabase,
        entry: Entry,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        registerInfo: RegisterInfo?,
        typeMode: TypeMode
    ) {
        removeSearch()
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
        entry: Entry,
        registerInfo: RegisterInfo
    ) {
        val newEntry = Entry(entry)
        val entryInfo = newEntry.getEntryInfo(
            database,
            raw = true,
            removeTemplateConfiguration = false
        )
        entryInfo.saveRegisterInfo(database, registerInfo)
        newEntry.setEntryInfo(database, entryInfo)
        updateEntry(entry, newEntry)
    }

    private fun Entry.containsSearchInfo(
        database: ContextualDatabase,
        searchInfo: SearchInfo
    ): Boolean {
        return getEntryInfo(
            database,
            raw = true,
            removeTemplateConfiguration = false
        ).containsSearchInfo(searchInfo)
    }

    private fun finishNodeAction() {
        actionNodeMode?.finish()
    }

    override fun onNodeSelected(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        if (nodes.isNotEmpty()) {
            if (actionNodeMode == null || toolbarAction?.getSupportActionModeCallback() == null) {
                mGroupFragment?.actionNodesCallback(
                    database,
                    nodes,
                    this
                ) { _ ->
                    actionNodeMode = null
                    addNodeButtonView?.showButton()
                }?.let {
                    actionNodeMode = toolbarAction?.startSupportActionMode(it)
                }
            } else {
                actionNodeMode?.invalidate()
            }
            addNodeButtonView?.hideButton()
        } else {
            finishNodeAction()
        }
        return true
    }

    override fun onOpenMenuClick(
        database: ContextualDatabase,
        node: Node
    ): Boolean {
        finishNodeAction()
        onNodeClick(database, node)
        return true
    }

    override fun onEditMenuClick(
        database: ContextualDatabase,
        node: Node
    ): Boolean {
        finishNodeAction()
        when (node.type) {
            Type.GROUP -> {
                launchDialogForGroupUpdate(node as Group)
            }
            Type.ENTRY -> {
                EntryEditActivity.launch(
                    activity = this@GroupActivity,
                    database = database,
                    registrationType = EntryEditActivity.RegistrationType.UPDATE,
                    nodeId = (node as Entry).nodeId,
                    activityResultLauncher = mEntryActivityResultLauncher
                )
            }
        }
        return true
    }

    private fun launchDialogToShowGroupInfo(group: Group) {
        GroupDialogFragment.launch(group.getGroupInfo())
            .show(supportFragmentManager, GroupDialogFragment.TAG_SHOW_GROUP)
    }

    private fun launchDialogForGroupCreation(group: Group) {
        GroupEditDialogFragment.create(GroupInfo().apply {
            if (group.allowAddNoteInGroup) {
                notes = ""
            }
        }).show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogForGroupUpdate(group: Group) {
        mOldGroupToUpdate = group
        GroupEditDialogFragment.update(group.getGroupInfo())
            .show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
    }

    private fun launchDialogToAskMainCredential(uri: Uri?) {
        MainCredentialDialogFragment.getInstance(uri)
            .show(supportFragmentManager, MainCredentialDialogFragment.TAG_ASK_MAIN_CREDENTIAL)
    }

    override fun onCopyMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()
        removeSearch()
        loadGroup()
        return true
    }

    override fun onMoveMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()
        removeSearch()
        loadGroup()
        return true
    }

    override fun onPasteMenuClick(
        database: ContextualDatabase,
        pasteMode: GroupFragment.PasteMode?,
        nodes: List<Node>
    ): Boolean {
        when (pasteMode) {
            GroupFragment.PasteMode.PASTE_FROM_COPY -> {
                // Copy
                mMainGroup?.let { newParent ->
                    copyNodes(nodes, newParent)
                }
            }
            GroupFragment.PasteMode.PASTE_FROM_MOVE -> {
                // Move
                mMainGroup?.let { newParent ->
                    moveNodes(nodes, newParent)
                }
            }
            else -> {
            }
        }
        finishNodeAction()
        return true
    }

    override fun onDeleteMenuClick(
        database: ContextualDatabase,
        nodes: List<Node>
    ): Boolean {
        deleteNodes(nodes)
        finishNodeAction()
        return true
    }

    override fun onAskMainCredentialDialogPositiveClick(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) {
        databaseUri?.let {
            mergeDatabaseFrom(it, mainCredential)
        }
    }

    override fun onAskMainCredentialDialogNegativeClick(
        databaseUri: Uri?,
        mainCredential: MainCredential
    ) { }

    override fun onResume() {
        super.onResume()

        // Show the lock button
        lockView?.visibility = if (PreferencesUtil.showLockDatabaseButton(this)) {
            View.VISIBLE
        } else {
            View.GONE
        }
        // Padding if lock button visible
        toolbarAction?.updateLockPaddingStart()

        loadGroup()
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
        searchView?.setOnQueryTextListener(null)
        if (!mTempSearchInfo) {
            searchFiltersView?.saveSearchParameters()
        }
    }

    private fun addSearchQueryInSearchView(searchQuery: String) {
        searchView?.setOnQueryTextListener(null)
        if (mAutoSearch)
            searchView?.clearFocus()
        searchView?.setQuery(searchQuery, false)
        searchView?.setOnQueryTextListener(mOnSearchQueryTextListener)
    }

    private fun prepareDatabaseNavMenu() {
        // hide or show nav menu
        databaseNavView?.apply {
            //  depending on current mode
            val modeCondition = mSpecialMode == SpecialMode.DEFAULT
            menu.findItem(R.id.menu_app_settings)?.isVisible = modeCondition
            menu.findItem(R.id.menu_merge_from)?.isVisible = mMergeDataAllowed && modeCondition
            menu.findItem(R.id.menu_save_copy_to)?.isVisible = modeCondition
            menu.findItem(R.id.menu_about)?.isVisible = modeCondition
            menu.findItem(R.id.menu_contribute)?.isVisible = modeCondition
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.database, menu)
        if (mDatabaseReadOnly) {
            menu.findItem(R.id.menu_save_database)?.isVisible = false
            menu.findItem(R.id.menu_merge_database)?.isVisible = false
        }
        if (!mMergeDataAllowed) {
            menu.findItem(R.id.menu_merge_database)?.isVisible = false
        }
        if (mSpecialMode != SpecialMode.DEFAULT) {
            menu.findItem(R.id.menu_merge_database)?.isVisible = false
            menu.findItem(R.id.menu_reload_database)?.isVisible = false
        }
        // Menu for recycle bin
        if (mRecyclingBinEnabled && mRecyclingBinIsCurrentGroup) {
            inflater.inflate(R.menu.recycle_bin, menu)
        }

        prepareDatabaseNavMenu()

        // Get the SearchView and set the searchable configuration
        menu.findItem(R.id.menu_search)?.let {
            mLockSearchListeners = true
            it.setOnActionExpandListener(mOnSearchActionExpandListener)
            searchView = it.actionView as SearchView?
            searchView?.apply {
                setOnQueryTextFocusChangeListener(mOnSearchTextFocusChangeListener)
                val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
                (searchManager?.getSearchableInfo(
                    ComponentName(this@GroupActivity, GroupActivity::class.java)
                ))?.let { searchableInfo ->
                    setSearchableInfo(searchableInfo)
                }
                val searchState = mSearchState
                // already open
                if (searchState != null) {
                    it.expandActionView()
                    addSearchQueryInSearchView(searchState.searchParameters.searchQuery)
                    if (mTempSearchInfo.not()) {
                        searchFiltersView?.searchParameters = searchState.searchParameters
                    }
                }
            }
            if (it.isActionViewExpanded) {
                breadcrumbListView?.visibility = View.GONE
                searchFiltersView?.visibility = View.VISIBLE
            } else {
                searchFiltersView?.visibility = View.GONE
                breadcrumbListView?.visibility = View.VISIBLE
            }
            mLockSearchListeners = false
            mAutoSearch = false
        }

        super.onCreateOptionsMenu(menu)

        // Launch education screen
        Handler(Looper.getMainLooper()).post {
            performedNextEducation(menu)
        }

        return true
    }

    private fun performedNextEducation(menu: Menu) {

        // If no node, show education to add new one
        val addNodeButtonEducationPerformed = actionNodeMode == null
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawerLayout?.openDrawer(GravityCompat.START)
                return true
            }
            R.id.menu_search -> {
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
                if (mRecyclingBinEnabled && mRecyclingBinIsCurrentGroup) {
                    mMainGroup?.getChildren()?.let { listChildren ->
                        // Automatically delete all elements
                        deleteNodes(listChildren, true)
                        finishNodeAction()
                    }
                }
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSortSelected(
        sortNodeEnum: SortNodeEnum,
        sortNodeParameters: SortNodeEnum.SortNodeParameters
    ) {
        mGroupFragment?.onSortSelected(sortNodeEnum, sortNodeParameters)
    }

    override fun onCancelSpecialMode() {
        super.onCancelSpecialMode()
        removeSearch()
        loadGroup()
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
        if (mGroupFragment?.nodeActionSelectionMode == true) {
            finishNodeAction()
        } else {
            // Normal way when we are not in root
            if (mRootGroup != null && mRootGroup != mCurrentGroup) {
                when {
                    Intent.ACTION_SEARCH == intent.action -> {
                        removeSearch()
                        loadGroup()
                    }
                    mPreviousGroupsIds.isEmpty() -> {
                        super.onRegularBackPressed()
                    }
                    else -> {
                        // Load the previous group
                        loadMainGroup(mPreviousGroupsIds
                            .removeAt(mPreviousGroupsIds.lastIndex))
                    }
                }
            }
            // Else in root, lock if needed
            else {
                removeSearch()
                intent.removeModes()
                intent.removeInfo()
                if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
                    lockAndExit()
                } else {
                    backToTheAppCaller()
                }
            }
        }
    }

    data class SearchState(
        var searchParameters: SearchParameters,
        var firstVisibleItem: Int?
    ) : Parcelable {

        private constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat<SearchParameters>() ?: SearchParameters(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(searchParameters, flags)
            parcel.writeInt(firstVisibleItem ?: 0)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<SearchState> {
            override fun createFromParcel(parcel: Parcel): SearchState {
                return SearchState(parcel)
            }

            override fun newArray(size: Int): Array<SearchState?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class GroupState(
        var groupId: NodeId<*>?,
        var firstVisibleItem: Int?
    ) : Parcelable {

        private constructor(parcel: Parcel) : this(
            parcel.readParcelableCompat<NodeId<*>>(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(groupId, flags)
            parcel.writeInt(firstVisibleItem ?: 0)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<GroupState> {
            override fun createFromParcel(parcel: Parcel): GroupState {
                return GroupState(parcel)
            }

            override fun newArray(size: Int): Array<GroupState?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {

        private val TAG = GroupActivity::class.java.name

        private const val REQUEST_STARTUP_SEARCH_KEY = "REQUEST_STARTUP_SEARCH_KEY"
        private const val GROUP_STATE_KEY = "GROUP_STATE_KEY"
        private const val PREVIOUS_GROUPS_IDS_KEY = "PREVIOUS_GROUPS_IDS_KEY"
        private const val GROUP_FRAGMENT_TAG = "GROUP_FRAGMENT_TAG"
        private const val OLD_GROUP_TO_UPDATE_KEY = "OLD_GROUP_TO_UPDATE_KEY"
        private const val AUTO_SEARCH_KEY = "AUTO_SEARCH_KEY"

        private fun buildIntent(
            context: Context,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            val intent = Intent(context, GroupActivity::class.java)
            if (groupState != null) {
                intent.putExtra(GROUP_STATE_KEY, groupState)
            }
            intentBuildLauncher.invoke(intent)
        }

        private fun checkTimeAndBuildIntent(
            context: Context,
            groupState: GroupState?,
            intentBuildLauncher: (Intent) -> Unit
        ) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(context)) {
                buildIntent(context, groupState, intentBuildLauncher)
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
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
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
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
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
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
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
                checkTimeAndBuildIntent(context, null) { intent ->
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
                        onItemsFound = { openedDatabase, items ->
                            when (typeMode) {
                                TypeMode.DEFAULT -> {}
                                TypeMode.MAGIKEYBOARD -> {
                                    MagikeyboardService.performSelection(
                                        items = items,
                                        actionPopulateKeyboard = { entryInfo ->
                                            activity.buildSpecialModeResponseAndSetResult(items)
                                            onValidateSpecialMode()
                                        },
                                        actionEntrySelection = { autoSearch ->
                                            launchForSelection(
                                                context = activity,
                                                database = database,
                                                typeMode = TypeMode.MAGIKEYBOARD,
                                                searchInfo = searchInfo,
                                                activityResultLauncher = activityResultLauncher,
                                                autoSearch = autoSearch
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
                                TypeMode.AUTOFILL -> {
                                    // Response is build
                                    activity.buildSpecialModeResponseAndSetResult(items)
                                    onValidateSpecialMode()
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
