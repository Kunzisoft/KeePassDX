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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.kunzisoft.keepass.credentialprovider.UserVerificationActionType
import com.kunzisoft.keepass.credentialprovider.UserVerificationData
import com.kunzisoft.keepass.credentialprovider.UserVerificationHelper.Companion.checkUserVerification
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.buildPasskeyResponseAndSetResult
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.buildPasswordResponseAndSetResult
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.model.DataTime
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchGroupInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_GROUP_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getNewEntry
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsActivity
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.BACK_PREVIOUS_KEYBOARD_ACTION
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboard
import com.kunzisoft.keepass.utils.TimeUtil.datePickerToDataDate
import com.kunzisoft.keepass.utils.UriUtil.openUrl
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
import com.kunzisoft.keepass.view.showError
import com.kunzisoft.keepass.view.toastError
import com.kunzisoft.keepass.view.updateButtonPaddingStart
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import com.kunzisoft.keepass.viewmodels.MainCredentialViewModel
import com.kunzisoft.keepass.viewmodels.UserVerificationViewModel
import kotlinx.coroutines.launch
import org.joda.time.LocalDateTime
import java.util.EnumSet


class GroupActivity : DatabaseLockActivity(),
        GroupFragment.NodeClickListener,
        GroupFragment.NodesActionMenuListener,
        GroupFragment.OnScrollListener,
        GroupFragment.GroupRefreshedListener,
        SortDialogFragment.SortSelectionListener {

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
    private val mMainCredentialViewModel: MainCredentialViewModel by viewModels()
    private val mUserVerificationViewModel: UserVerificationViewModel by viewModels()

    private val mGroupActivityEducation = GroupActivityEducation(this)

    private var mBreadcrumbAdapter: BreadcrumbAdapter? = null

    private var mGroupFragment: GroupFragment? = null
    private var mRecyclingBinEnabled = false

    private var actionNodeMode: ActionMode? = null

    // Manage merge
    private var mExternalFileHelper: ExternalFileHelper? = null

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
            mGroupViewModel.searchText(mDatabase, newText)
            return true
        }
    }
    private val mOnSearchFiltersChangeListener = object : ((SearchParameters) -> Unit) {
        override fun invoke(searchParameters: SearchParameters) {
            mGroupViewModel.searchWithParameters(mDatabase, searchParameters)
        }
    }
    private val mOnSearchActionExpandListener = object : MenuItem.OnActionExpandListener {
        override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
            searchFiltersView?.visibility = View.VISIBLE
            searchFiltersView?.allowAdvancedSearch(!mGroupViewModel.mTempSearchInfo)
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
        if (!mGroupViewModel.mAutoSearch
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
        mGroupViewModel.assignSearchParameters(searchFiltersView?.searchParameters)
    }

    private fun removeSearch() {
        mGroupViewModel.clearSearch()
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

        searchFiltersView?.closeAdvancedFilters()

        // Retrieve group if defined at launch
        manageIntent(intent)

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mGroupViewModel.onGroupLoaded.collect { group ->
                        if (group !is SearchGroupInfo)
                            touchGroup(group)
                        loadingView?.hideByFading()
                    }
                }
                launch {
                    mGroupEditViewModel.requestIconSelection.collect { iconImage ->
                        IconPickerActivity.launch(this@GroupActivity, iconImage, mIconSelectionActivityResultLauncher)
                    }
                }
                launch {
                    mGroupEditViewModel.requestDateTimeSelection.collect { dateInstant ->
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
                }
                launch {
                    mMainCredentialViewModel.uiState.collect { credentialState ->
                        when (credentialState) {
                            is MainCredentialViewModel.UIState.Loading -> {}
                            is MainCredentialViewModel.UIState.OnMainCredentialEntered -> {
                                mergeDatabaseFrom(credentialState.databaseUri, credentialState.mainCredential)
                                mMainCredentialViewModel.onActionReceived()
                            }
                            is MainCredentialViewModel.UIState.OnMainCredentialCanceled -> {
                                mMainCredentialViewModel.onActionReceived()
                            }
                        }
                    }
                }
                launch {
                    mGroupEditViewModel.onCreateGroup.collect { groupInfo ->
                        mGroupViewModel.mMainGroup?.nodeId?.let { parentId ->
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
                mGroupViewModel.mMainGroup?.nodeId?.let { currentGroupId ->
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

    private fun loadGroup() {
        mGroupViewModel.mSearchState?.let { searchState ->
            finishNodeAction()
            mGroupViewModel.loadSearch(
                mDatabase,
                searchState
            )
        } ?: mGroupViewModel.mMainGroupState?.let { currentGroupState ->
            mGroupViewModel.loadMainGroup(
                mDatabase,
                currentGroupState.groupId,
                currentGroupState.firstVisibleItem
            )
        } ?: run {
            mGroupViewModel.loadMainGroup(
                mDatabase,
                null,
                0
            )
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        super.onDatabaseRetrieved(database)

        mBreadcrumbAdapter = BreadcrumbAdapter(this, database).apply {
            // Open group on breadcrumb click
            onItemClickListener = { node, _ ->
                // If last item & not a virtual root group
                val currentGroup = mGroupViewModel.mMainGroup
                if (currentGroup != null && node.nodeId == currentGroup.nodeId
                    && (currentGroup.nodeId != mDatabase?.rootGroup?.nodeId
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
                val currentGroup = mGroupViewModel.mMainGroup
                if (currentGroup != null && node.nodeId == currentGroup.nodeId
                    && (currentGroup.nodeId != mDatabase?.rootGroup?.nodeId
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

        mGroupEditViewModel.groupNamesNotAllowed = database.groupNamesNotAllowed

        mRecyclingBinEnabled = !mDatabaseReadOnly && database.isRecycleBinEnabled == true

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
        if (actionTask == ACTION_DATABASE_UPDATE_GROUP_TASK
            || actionTask == ACTION_DATABASE_UPDATE_ENTRY_TASK) {
            if (result.isSuccess) {
                coordinatorError?.showActionErrorIfNeeded(result)
                // Reload the group
                loadGroup()
                finishNodeAction()
            }
        }
    }

    private fun manageIntent(intent: Intent?) {
        intent?.let {
            // To get the form filling search as temp search
            val searchInfo: SearchInfo? = intent.retrieveSearchInfo()
            val autoSearch = intent.getBooleanExtra(AUTO_SEARCH_KEY, false)
            // Get search query
            if (searchInfo != null && autoSearch) {
                mGroupViewModel.mAutoSearch = true
                mGroupViewModel.mTempSearchInfo = true
                searchInfo.getSearchParametersFromSearchInfo(this) {
                    mGroupViewModel.mSearchState = SearchState(
                        searchParameters = it,
                        firstVisibleItem = mGroupViewModel.mSearchState?.firstVisibleItem ?: 0
                    )
                }
            } else if (intent.action == Intent.ACTION_SEARCH) {
                mGroupViewModel.mAutoSearch = true
                mGroupViewModel.mSearchState = SearchState(
                    searchParameters = PreferencesUtil.getDefaultSearchParameters(this).apply {
                        searchQuery = intent.getStringExtra(SearchManager.QUERY)
                            ?.trim { it <= ' ' } ?: ""
                    },
                    firstVisibleItem = mGroupViewModel.mSearchState?.firstVisibleItem ?: 0
                )
            } else if (mGroupViewModel.mRequestStartupSearch
                && PreferencesUtil.automaticallyFocusSearch(this@GroupActivity)) {
                // Expand the search view if defined in settings
                // To request search only one time
                mGroupViewModel.mRequestStartupSearch = false
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

    override fun onGroupRefreshed() {
        val group = mGroupViewModel.mCurrentGroup
        // Assign title
        if (group is SearchGroupInfo) {
            val tags = mDatabase?.tagPoolWithoutHistory
            searchFiltersView?.apply {
                setNumbers(group.numberOfSearchResults())
                setSelectableTags(tags)
                setCurrentGroupText(mGroupViewModel.mMainGroup?.title ?: getString(R.string.search))
                availableOther(mDatabase?.allowEntryCustomFields() ?: false)
                availableApplicationIds(mDatabase?.allowEntryCustomFields() ?: false)
                availableTags(mDatabase?.allowTags() ?: false)
                enableTags(tags?.isNotEmpty() ?: false)
                availableSearchableGroup(mDatabase?.allowCustomSearchableGroup() ?: false)
                availableTemplates(mDatabase?.allowTemplatesGroup ?: false)
                enableTemplates(mDatabase?.templatesGroup != null)
            }
        } else {
            // Add breadcrumb
            setBreadcrumbNode(group)
            refreshDatabaseViews()
            invalidateOptionsMenu()
        }
        initAddButton(group)
    }

    private fun setBreadcrumbNode(group: GroupInfo?) {
        mBreadcrumbAdapter?.apply {
            // TODO Call view model
            group?.let {
                database?.getBreadcrumb(it)?.let { breadcrumb ->
                    setNode(breadcrumb)
                }
            }
            breadcrumbListView?.scrollToPosition(itemCount -1)
        }
    }

    private fun initAddButton(group: GroupInfo?) {
        addNodeButtonView?.apply {
            closeButtonIfOpen()
            // To enable add button
            val addGroupEnabled = mDatabase?.allowAddGroupIn(
                group = group,
                forceReadOnly = mDatabaseReadOnly
            ) ?: false
            val addEntryEnabled = mDatabase?.allowAddEntryIn(
                group = group,
                forceReadOnly = mDatabaseReadOnly
            ) ?: false
            enableAddGroup(addGroupEnabled)
            enableAddEntry(addEntryEnabled)
            if (addGroupEnabled.not() && addEntryEnabled.not())
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
        node: NodeInfo
    ) {
        when (node) {
            is GroupInfo -> {
                // Open child group
                mGroupViewModel.loadChildGroup(database, node.nodeId)
            }
            is EntryInfo -> {
                EntrySelectionHelper.doSpecialAction(
                    intent = intent,
                    defaultAction = {
                        EntryActivity.launch(
                            activity = this@GroupActivity,
                            database = database,
                            entryId = node.nodeId,
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
                                    && node.allowedToSaveSearchInfo(searchInfo)
                                    && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@GroupActivity)
                                ) {
                                    updateEntryWithRegisterInfo(
                                        database = database,
                                        entry = node,
                                        registerInfo = searchInfo!!.toRegisterInfo()
                                    )
                                } else {
                                    entrySelectedForSelection(node)
                                }
                            }
                            TypeMode.AUTOFILL -> {
                                if (!database.isReadOnly
                                    && node.allowedToSaveSearchInfo(searchInfo)
                                    && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@GroupActivity)
                                ) {
                                    updateEntryWithRegisterInfo(
                                        database = database,
                                        entry = node,
                                        registerInfo = searchInfo!!.toRegisterInfo()
                                    )
                                } else {
                                    entrySelectedForSelection(node)
                                }
                            }
                            TypeMode.PASSWORD -> {
                                entrySelectedForPasswordSelection(node)
                            }
                            TypeMode.PASSKEY -> {
                                entrySelectedForPasskeySelection(node)
                            }
                        }
                        loadGroup()
                    },
                    registrationAction = { intentSenderMode, typeMode, registerInfo ->
                        if (!database.isReadOnly) {
                            entrySelectedForRegistration(
                                database = database,
                                entry = node,
                                registerInfo = registerInfo,
                                typeMode = typeMode,
                                activityResultLauncher = if (intentSenderMode)
                                    mCredentialActivityResultLauncher else null
                            )
                            loadGroup()
                        } else
                            finish()
                    })
            }
        }
    }

    private fun entrySelectedForSelection(entry: EntryInfo) {
        removeSearch()
        // Build response with the entry selected
        this.buildSpecialModeResponseAndSetResult(entry)
        onValidateSpecialMode()
    }

    private fun entrySelectedForPasswordSelection(entry: EntryInfo) {
        removeSearch()
        // Build response with the entry selected
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            buildPasswordResponseAndSetResult(entryInfo = entry)
        }
        onValidateSpecialMode()
    }

    private fun entrySelectedForPasskeySelection(entry: EntryInfo) {
        removeSearch()
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
        entry: EntryInfo,
        registerInfo: RegisterInfo
    ) {
        database.saveRegisterInfoIn(entry, registerInfo)
        updateEntry(entry)
    }

    private fun editEntry(database: ContextualDatabase?, entryId: NodeId<*>?) {
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
        actionNodeMode?.finish()
    }

    override fun onNodeSelected(
        database: ContextualDatabase,
        nodes: List<NodeInfo>
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
        node: NodeInfo
    ): Boolean {
        finishNodeAction()
        onNodeClick(database, node)
        return true
    }

    override fun onEditMenuClick(
        database: ContextualDatabase,
        node: NodeInfo
    ): Boolean {
        finishNodeAction()
        when (node) {
            is GroupInfo -> {
                launchDialogForGroupUpdate(node)
            }
            is EntryInfo -> {
                if (mDatabaseAllowUserVerification) {
                    checkUserVerification(
                        userVerificationViewModel = mUserVerificationViewModel,
                        dataToVerify = UserVerificationData(
                            actionType = UserVerificationActionType.EDIT_ENTRY,
                            database = database,
                            entryId = node.nodeId
                        )
                    )
                } else {
                    editEntry(database, node.nodeId)
                }
            }
        }
        return true
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

    override fun onCopyMenuClick(
        database: ContextualDatabase,
        nodes: List<NodeInfo>
    ): Boolean {
        actionNodeMode?.invalidate()
        removeSearch()
        loadGroup()
        return true
    }

    override fun onMoveMenuClick(
        database: ContextualDatabase,
        nodes: List<NodeInfo>
    ): Boolean {
        actionNodeMode?.invalidate()
        removeSearch()
        loadGroup()
        return true
    }

    override fun onPasteMenuClick(
        database: ContextualDatabase,
        pasteMode: GroupFragment.PasteMode?,
        nodes: List<NodeInfo>
    ): Boolean {
        when (pasteMode) {
            GroupFragment.PasteMode.PASTE_FROM_COPY -> {
                // Copy
                mGroupViewModel.mMainGroup?.nodeId?.let { newParentId ->
                    copyNodes(newParentId, nodes)
                }
            }
            GroupFragment.PasteMode.PASTE_FROM_MOVE -> {
                // Move
                mGroupViewModel.mMainGroup?.nodeId?.let { newParentId ->
                    moveNodes(newParentId, nodes)
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
        nodes: List<NodeInfo>
    ): Boolean {
        deleteNodes(nodes)
        finishNodeAction()
        return true
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

    override fun onPause() {
        super.onPause()

        finishNodeAction()
        searchView?.setOnQueryTextListener(null)
        if (!mGroupViewModel.mTempSearchInfo) {
            searchFiltersView?.saveSearchParameters()
        }
    }

    private fun addSearchQueryInSearchView(searchQuery: String) {
        searchView?.setOnQueryTextListener(null)
        if (mGroupViewModel.mAutoSearch)
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
        if (mRecyclingBinEnabled && mGroupViewModel.recyclingBinIsCurrentGroup) {
            inflater.inflate(R.menu.recycle_bin, menu)
        }

        prepareDatabaseNavMenu()

        // Get the SearchView and set the searchable configuration
        menu.findItem(R.id.menu_search)?.let {
            mGroupViewModel.mLockSearchListeners = true
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
                val searchState = mGroupViewModel.mSearchState
                // already open
                if (searchState != null) {
                    it.expandActionView()
                    addSearchQueryInSearchView(searchState.searchParameters.searchQuery)
                    if (mGroupViewModel.mTempSearchInfo.not()) {
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
            mGroupViewModel.mLockSearchListeners = false
            mGroupViewModel.mAutoSearch = false
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
                if (mRecyclingBinEnabled && mGroupViewModel.recyclingBinIsCurrentGroup) {
                    emptyRecycleBin()
                    finishNodeAction()
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
            if (mGroupViewModel.isCurrentGroupRoot(mDatabase)) {
                // In root, lock if needed
                removeSearch()
                intent.removeModes()
                intent.removeInfo()
                if (PreferencesUtil.isLockDatabaseWhenBackButtonOnRootClicked(this)) {
                    lockAndExit()
                } else {
                    backToTheAppCaller()
                }
            } else {
                // Normal way when we are not in root
                when {
                    Intent.ACTION_SEARCH == intent.action -> {
                        removeSearch()
                        loadGroup()
                    }
                    mGroupViewModel.previousGroupExists() -> {
                        super.onRegularBackPressed()
                    }
                    else -> {
                        // Load the previous group
                        mGroupViewModel.loadPreviousGroup(mDatabase)
                    }
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

        private const val GROUP_FRAGMENT_TAG = "GROUP_FRAGMENT_TAG"
        private const val AUTO_SEARCH_KEY = "AUTO_SEARCH_KEY"

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
                checkTimeAndBuildIntent(context) { intent ->
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
                checkTimeAndBuildIntent(context) { intent ->
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
