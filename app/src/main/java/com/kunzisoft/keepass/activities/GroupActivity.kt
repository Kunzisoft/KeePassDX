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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.BreadcrumbAdapter
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
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
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.NEW_NODES_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getListNodesFromBundle
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard
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
import com.kunzisoft.keepass.view.updateLockPaddingLeft
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel


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

    private var mAutofillActivityResultLauncher: ActivityResultLauncher<Intent>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            AutofillHelper.buildActivityResultLauncher(this)
        else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.activity_group, null))

        // Initialize views
        header = findViewById(R.id.activity_group_header)
        footer = findViewById(R.id.activity_group_footer)
        drawerLayout = findViewById(R.id.drawer_layout)
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
            drawerLayout?.applyWindowInsets(WindowInsetPosition.TOP_BOTTOM_IME)
            footer?.applyWindowInsets(WindowInsetPosition.BOTTOM_IME)
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
                            getString(R.string.database_file_name_copy) +
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

        mBreadcrumbAdapter = BreadcrumbAdapter(this).apply {
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
                        mGroupEditViewModel.selectTime(this.hour, this.minute)
                    }
                    show(supportFragmentManager, "TimePickerFragment")
                }
            } else {
                // Launch the date picker
                MaterialDatePicker.Builder.datePicker().build().apply {
                    addOnPositiveButtonClickListener {
                        mGroupEditViewModel.selectDate(it)
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
                    EntrySelectionHelper.doSpecialAction(intent,
                        {
                            mMainGroup?.nodeId?.let { currentParentGroupId ->
                                EntryEditActivity.launchToCreate(
                                    this@GroupActivity,
                                    database,
                                    currentParentGroupId,
                                    mEntryActivityResultLauncher
                                )
                            }
                        },
                        {
                            // Search not used
                        },
                        { searchInfo ->
                            EntryEditActivity.launchToCreateForSave(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo ->
                            EntryEditActivity.launchForKeyboardSelectionResult(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
                            )
                            onLaunchActivitySpecialMode()
                        },
                        { searchInfo, autofillComponent ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                EntryEditActivity.launchForAutofillResult(
                                    this@GroupActivity,
                                    database,
                                    mAutofillActivityResultLauncher,
                                    autofillComponent,
                                    currentGroup.nodeId,
                                    searchInfo
                                )
                                onLaunchActivitySpecialMode()
                            } else {
                                onCancelSpecialMode()
                            }
                        },
                        { searchInfo ->
                            EntryEditActivity.launchToCreateForRegistration(
                                this@GroupActivity,
                                database,
                                currentGroup.nodeId,
                                searchInfo
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

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        mGroupEditViewModel.setGroupNamesNotAllowed(database?.groupNamesNotAllowed)

        mRecyclingBinEnabled = !mDatabaseReadOnly
                && database?.isRecycleBinEnabled == true

        mRootGroup = database?.rootGroup
        loadGroup()

        // Update view
        database?.let {
            mBreadcrumbAdapter?.iconDrawableFactory = it.iconDrawableFactory
        }
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
            result.data?.getBundle(NEW_NODES_KEY)?.let { newNodesBundle ->
                entry = getListNodesFromBundle(database, newNodesBundle)[0] as Entry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve entry action for selection", e)
        }

        when (actionTask) {
            ACTION_DATABASE_UPDATE_ENTRY_TASK -> {
                if (result.isSuccess) {
                    EntrySelectionHelper.doSpecialAction(intent,
                        {
                            // Standard not used after task
                        },
                        {
                            // Search not used
                        },
                        {
                            // Save not used
                        },
                        {
                            // Keyboard selection
                            entry?.let {
                                entrySelectedForKeyboardSelection(database, it)
                            }
                        },
                        { _, _ ->
                            // Autofill selection
                            entry?.let {
                                entrySelectedForAutofillSelection(database, it)
                            }
                        },
                        {
                            // Not use
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

    /**
     * Transform the AUTO_SEARCH_KEY in ACTION_SEARCH, return true if AUTO_SEARCH_KEY was present
     */
    private fun transformSearchInfoIntent(intent: Intent) {
        // To relaunch the activity as ACTION_SEARCH
        val searchInfo: SearchInfo? = EntrySelectionHelper.retrieveSearchInfoFromIntent(intent)
        val autoSearch = intent.getBooleanExtra(AUTO_SEARCH_KEY, false)
        intent.removeExtra(AUTO_SEARCH_KEY)
        if (searchInfo != null && autoSearch) {
            intent.action = Intent.ACTION_SEARCH
            intent.putExtra(SearchManager.QUERY, searchInfo.toString())
        }
    }

    private fun manageIntent(intent: Intent?) {
        intent?.let {
            if (intent.extras?.containsKey(GROUP_STATE_KEY) == true) {
                mMainGroupState = intent.getParcelableExtraCompat(GROUP_STATE_KEY)
                intent.removeExtra(GROUP_STATE_KEY)
            }
            // To transform KEY_SEARCH_INFO in ACTION_SEARCH
            transformSearchInfoIntent(intent)
            // Get search query
            if (intent.action == Intent.ACTION_SEARCH) {
                mAutoSearch = true
                val stringQuery = intent.getStringExtra(SearchManager.QUERY)?.trim { it <= ' ' } ?: ""
                intent.action = Intent.ACTION_DEFAULT
                intent.removeExtra(SearchManager.QUERY)
                mSearchState = SearchState(PreferencesUtil.getDefaultSearchParameters(this).apply {
                    searchQuery = stringQuery
                }, mSearchState?.firstVisibleItem ?: 0)
            } else if (mRequestStartupSearch
                && PreferencesUtil.automaticallyFocusSearch(this@GroupActivity)) {
                // Expand the search view if defined in settings
                // To request search only one time
                mRequestStartupSearch = false
                addSearch()
            }
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
            searchFiltersView?.setCurrentGroupText(mMainGroup?.title ?: "")
            searchFiltersView?.availableOther(mDatabase?.allowEntryCustomFields() ?: false)
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
                Log.e(TAG, "Node can't be cast in Group")
            }

            Type.ENTRY -> try {
                val entryVersioned = node as Entry
                EntrySelectionHelper.doSpecialAction(intent,
                    {
                        EntryActivity.launch(
                            this@GroupActivity,
                            database,
                            entryVersioned.nodeId,
                            mEntryActivityResultLauncher
                        )
                        // Do not reload group here
                    },
                    {
                        // Nothing here, a search is simply performed
                    },
                    { searchInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForSave(database, entryVersioned, searchInfo)
                            loadGroup()
                        } else
                            finish()
                    },
                    { searchInfo ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@GroupActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForKeyboardSelection(database, entryVersioned)
                        loadGroup()
                    },
                    { searchInfo, _ ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@GroupActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForAutofillSelection(database, entryVersioned)
                        loadGroup()
                    },
                    { registerInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForRegistration(database, entryVersioned, registerInfo)
                            loadGroup()
                        } else
                            finish()
                    })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    private fun entrySelectedForSave(database: ContextualDatabase, entry: Entry, searchInfo: SearchInfo) {
        removeSearch()
        // Save to update the entry
        EntryEditActivity.launchToUpdateForSave(
            this@GroupActivity,
            database,
            entry.nodeId,
            searchInfo
        )
        onLaunchActivitySpecialMode()
    }

    private fun entrySelectedForKeyboardSelection(database: ContextualDatabase, entry: Entry) {
        removeSearch()
        // Populate Magikeyboard with entry
        MagikeyboardService.populateKeyboardAndMoveAppToBackground(
            this,
            entry.getEntryInfo(database)
        )
        onValidateSpecialMode()
    }

    private fun entrySelectedForAutofillSelection(database: ContextualDatabase, entry: Entry) {
        removeSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.buildResponseAndSetResult(
                this,
                database,
                entry.getEntryInfo(database)
            )
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForRegistration(
        database: ContextualDatabase,
        entry: Entry,
        registerInfo: RegisterInfo?
    ) {
        removeSearch()
        // Registration to update the entry
        EntryEditActivity.launchToUpdateForRegistration(
            this@GroupActivity,
            database,
            entry.nodeId,
            registerInfo
        )
        onLaunchActivitySpecialMode()
    }

    private fun updateEntryWithSearchInfo(
        database: ContextualDatabase,
        entry: Entry,
        searchInfo: SearchInfo
    ) {
        val newEntry = Entry(entry)
        val entryInfo = newEntry.getEntryInfo(
            database,
            raw = true,
            removeTemplateConfiguration = false
        )
        val modification = entryInfo.saveSearchInfo(database, searchInfo)
        newEntry.setEntryInfo(database, entryInfo)
        if (modification) {
            updateEntry(entry, newEntry)
        }
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
                EntryEditActivity.launchToUpdate(
                    this@GroupActivity,
                    database,
                    (node as Entry).nodeId,
                    mEntryActivityResultLauncher
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
        toolbarAction?.updateLockPaddingLeft()

        loadGroup()
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
        searchView?.setOnQueryTextListener(null)
        searchFiltersView?.saveSearchParameters()
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
                val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?
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
                    searchFiltersView?.searchParameters = searchState.searchParameters
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
                        loadMainGroup(mPreviousGroupsIds.removeLast())
                    }
                }
            }
            // Else in root, lock if needed
            else {
                removeSearch()
                EntrySelectionHelper.removeModesFromIntent(intent)
                EntrySelectionHelper.removeInfoFromIntent(intent)
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

        private fun buildIntent(context: Context,
                                groupState: GroupState?,
                                intentBuildLauncher: (Intent) -> Unit) {
            val intent = Intent(context, GroupActivity::class.java)
            if (groupState != null) {
                intent.putExtra(GROUP_STATE_KEY, groupState)
            }
            intentBuildLauncher.invoke(intent)
        }

        private fun checkTimeAndBuildIntent(activity: Activity,
                                            groupState: GroupState?,
                                            intentBuildLauncher: (Intent) -> Unit) {
            if (TimeoutHelper.checkTimeAndLockIfTimeout(activity)) {
                buildIntent(activity, groupState, intentBuildLauncher)
            }
        }

        private fun checkTimeAndBuildIntent(context: Context,
                                            groupState: GroupState?,
                                            intentBuildLauncher: (Intent) -> Unit) {
            if (TimeoutHelper.checkTime(context)) {
                buildIntent(context, groupState, intentBuildLauncher)
            }
        }

        /*
         * -------------------------
         * 		Standard Launch
         * -------------------------
         */
        fun launch(context: Context,
                   database: ContextualDatabase,
                   autoSearch: Boolean = false) {
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
        fun launchForSearchResult(context: Context,
                                  database: ContextualDatabase,
                                  searchInfo: SearchInfo,
                                  autoSearch: Boolean = false) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.addSearchInfoInIntent(
                        intent,
                        searchInfo
                    )
                    context.startActivity(intent)
                }
            }
        }

        /*
         * -------------------------
         * 		Search save Launch
         * -------------------------
         */
        fun launchForSaveResult(context: Context,
                                database: ContextualDatabase,
                                searchInfo: SearchInfo,
                                autoSearch: Boolean = false) {
            if (database.loaded && !database.isReadOnly) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.startActivityForSaveModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Keyboard Launch
         * -------------------------
         */
        fun launchForKeyboardSelectionResult(context: Context,
                                             database: ContextualDatabase,
                                             searchInfo: SearchInfo? = null,
                                             autoSearch: Boolean = false) {
            if (database.loaded) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    EntrySelectionHelper.startActivityForKeyboardSelectionModeResult(
                        context,
                        intent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Autofill Launch
         * -------------------------
         */
        @RequiresApi(api = Build.VERSION_CODES.O)
        fun launchForAutofillResult(activity: AppCompatActivity,
                                    database: ContextualDatabase,
                                    activityResultLaunch: ActivityResultLauncher<Intent>?,
                                    autofillComponent: AutofillComponent,
                                    searchInfo: SearchInfo? = null,
                                    autoSearch: Boolean = false) {
            if (database.loaded) {
                checkTimeAndBuildIntent(activity, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
                        activityResultLaunch,
                        autofillComponent,
                        searchInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Registration Launch
         * -------------------------
         */
        fun launchForRegistration(context: Context,
                                  database: ContextualDatabase,
                                  registerInfo: RegisterInfo? = null) {
            if (database.loaded && !database.isReadOnly) {
                checkTimeAndBuildIntent(context, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, false)
                    EntrySelectionHelper.startActivityForRegistrationModeResult(
                        context,
                        intent,
                        registerInfo
                    )
                }
            }
        }

        /*
         * -------------------------
         * 		Global Launch
         * -------------------------
         */
        fun launch(activity: AppCompatActivity,
                   database: ContextualDatabase,
                   onValidateSpecialMode: () -> Unit,
                   onCancelSpecialMode: () -> Unit,
                   onLaunchActivitySpecialMode: () -> Unit,
                   autofillActivityResultLauncher: ActivityResultLauncher<Intent>?) {
            EntrySelectionHelper.doSpecialAction(activity.intent,
                    {
                        // Default action
                        launch(
                            activity,
                            database,
                            true
                        )
                    },
                    { searchInfo ->
                        // Search action
                        if (database.loaded) {
                            launchForSearchResult(activity,
                                database,
                                searchInfo,
                                true)
                            onLaunchActivitySpecialMode()
                        } else {
                            // Simply close if database not opened
                            onCancelSpecialMode()
                        }
                    },
                    { searchInfo ->
                        // Save info
                        if (database.loaded) {
                            if (!database.isReadOnly) {
                                launchForSaveResult(
                                    activity,
                                    database,
                                    searchInfo,
                                    false
                                )
                                onLaunchActivitySpecialMode()
                            } else {
                                Toast.makeText(
                                    activity.applicationContext,
                                    R.string.autofill_read_only_save,
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                onCancelSpecialMode()
                            }
                        }
                    },
                    { searchInfo ->
                        // Keyboard selection
                        SearchHelper.checkAutoSearchInfo(activity,
                                database,
                                searchInfo,
                                { _, items ->
                                    MagikeyboardService.performSelection(
                                        items,
                                        { entryInfo ->
                                            // Keyboard populated
                                            MagikeyboardService.populateKeyboardAndMoveAppToBackground(
                                                activity,
                                                entryInfo
                                            )
                                            onValidateSpecialMode()
                                        },
                                        { autoSearch ->
                                            launchForKeyboardSelectionResult(activity,
                                                database,
                                                searchInfo,
                                                autoSearch)
                                            onLaunchActivitySpecialMode()
                                        }
                                    )
                                },
                                {
                                    // Here no search info found, disable auto search
                                    launchForKeyboardSelectionResult(activity,
                                        database,
                                        searchInfo,
                                        false)
                                    onLaunchActivitySpecialMode()
                                },
                                {
                                    // Simply close if database not opened, normally not happened
                                    onCancelSpecialMode()
                                }
                        )
                    },
                    { searchInfo, autofillComponent ->
                        // Autofill selection
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            SearchHelper.checkAutoSearchInfo(activity,
                                    database,
                                    searchInfo,
                                    { openedDatabase, items ->
                                        // Response is build
                                        AutofillHelper.buildResponseAndSetResult(activity, openedDatabase, items)
                                        onValidateSpecialMode()
                                    },
                                    {
                                        // Here no search info found, disable auto search
                                        launchForAutofillResult(activity,
                                            database,
                                            autofillActivityResultLauncher,
                                            autofillComponent,
                                            searchInfo,
                                            false)
                                        onLaunchActivitySpecialMode()
                                    },
                                    {
                                        // Simply close if database not opened, normally not happened
                                        onCancelSpecialMode()
                                    }
                            )
                        } else {
                            onCancelSpecialMode()
                        }
                    },
                    { registerInfo ->
                        // Autofill registration
                        if (!database.isReadOnly) {
                            SearchHelper.checkAutoSearchInfo(activity,
                                    database,
                                    registerInfo?.searchInfo,
                                    { _, _ ->
                                        // No auto search, it's a registration
                                        launchForRegistration(activity,
                                            database,
                                            registerInfo)
                                        onLaunchActivitySpecialMode()
                                    },
                                    {
                                        // Here no search info found, disable auto search
                                        launchForRegistration(activity,
                                            database,
                                            registerInfo)
                                        onLaunchActivitySpecialMode()
                                    },
                                    {
                                        // Simply close if database not opened, normally not happened
                                        onCancelSpecialMode()
                                    }
                            )
                        } else {
                            Toast.makeText(activity.applicationContext,
                                    R.string.autofill_read_only_save,
                                    Toast.LENGTH_LONG)
                                    .show()
                            onCancelSpecialMode()
                        }
                    })
        }
    }
}
