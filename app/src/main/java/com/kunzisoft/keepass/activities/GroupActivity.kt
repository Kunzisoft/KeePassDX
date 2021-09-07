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
import android.app.DatePickerDialog
import android.app.SearchManager
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.*
import com.kunzisoft.keepass.activities.fragments.GroupFragment
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.activities.helpers.SpecialMode
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.adapters.SearchEntryCursorAdapter
import com.kunzisoft.keepass.autofill.AutofillComponent
import com.kunzisoft.keepass.autofill.AutofillHelper
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.node.Node
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.education.GroupActivityEducation
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.ACTION_DATABASE_UPDATE_ENTRY_TASK
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.NEW_NODES_KEY
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getListNodesFromBundle
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
import com.kunzisoft.keepass.timeout.TimeoutHelper
import com.kunzisoft.keepass.utils.MenuUtil
import com.kunzisoft.keepass.view.*
import com.kunzisoft.keepass.viewmodels.GroupEditViewModel
import com.kunzisoft.keepass.viewmodels.GroupViewModel
import org.joda.time.DateTime

class GroupActivity : DatabaseLockActivity(),
        DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener,
        GroupFragment.NodeClickListener,
        GroupFragment.NodesActionMenuListener,
        GroupFragment.OnScrollListener,
        SortDialogFragment.SortSelectionListener {

    // Views
    private var rootContainerView: ViewGroup? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var lockView: View? = null
    private var toolbar: Toolbar? = null
    private var searchTitleView: View? = null
    private var toolbarAction: ToolbarAction? = null
    private var iconView: ImageView? = null
    private var numberChildrenView: TextView? = null
    private var addNodeButtonView: AddNodeButtonView? = null
    private var groupNameView: TextView? = null
    private var groupMetaView: TextView? = null
    private var loadingView: ProgressBar? = null

    private val mGroupViewModel: GroupViewModel by viewModels()
    private val mGroupEditViewModel: GroupEditViewModel by viewModels()

    private var mGroupFragment: GroupFragment? = null
    private var mRecyclingBinEnabled = false
    private var mRecyclingBinIsCurrentGroup = false
    private var mRequestStartupSearch = true

    private var actionNodeMode: ActionMode? = null

    // Nodes
    private var mCurrentGroupState: GroupState? = null
    private var mRootGroup: Group? = null
    private var mCurrentGroup: Group? = null
    private var mPreviousGroupsIds = mutableListOf<GroupState>()
    private var mOldGroupToUpdate: Group? = null

    private var mSearchSuggestionAdapter: SearchEntryCursorAdapter? = null
    private var mOnSuggestionListener: SearchView.OnSuggestionListener? = null

    private var mIconColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Construct main view
        setContentView(layoutInflater.inflate(R.layout.activity_group, null))

        // Initialize views
        rootContainerView = findViewById(R.id.activity_group_container_view)
        coordinatorLayout = findViewById(R.id.group_coordinator)
        iconView = findViewById(R.id.group_icon)
        numberChildrenView = findViewById(R.id.group_numbers)
        addNodeButtonView = findViewById(R.id.add_node_button)
        toolbar = findViewById(R.id.toolbar)
        searchTitleView = findViewById(R.id.search_title)
        groupNameView = findViewById(R.id.group_name)
        groupMetaView = findViewById(R.id.group_meta)
        toolbarAction = findViewById(R.id.toolbar_action)
        lockView = findViewById(R.id.lock_button)
        loadingView = findViewById(R.id.loading)

        lockView?.setOnClickListener {
            lockAndExit()
        }

        toolbar?.title = ""
        setSupportActionBar(toolbar)

        // Retrieve the textColor to tint the icon
        val taTextColor = theme.obtainStyledAttributes(intArrayOf(R.attr.textColorInverse))
        mIconColor = taTextColor.getColor(0, Color.WHITE)
        taTextColor.recycle()

        // Retrieve group if defined at launch
        manageIntent(intent)

        // Retrieve elements after an orientation change
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(REQUEST_STARTUP_SEARCH_KEY)) {
                mRequestStartupSearch = savedInstanceState.getBoolean(REQUEST_STARTUP_SEARCH_KEY)
                savedInstanceState.remove(REQUEST_STARTUP_SEARCH_KEY)
            }
            if (savedInstanceState.containsKey(OLD_GROUP_TO_UPDATE_KEY)) {
                mOldGroupToUpdate = savedInstanceState.getParcelable(OLD_GROUP_TO_UPDATE_KEY)
                savedInstanceState.remove(OLD_GROUP_TO_UPDATE_KEY)
            }
        }

        // Retrieve previous groups
        if (savedInstanceState != null && savedInstanceState.containsKey(PREVIOUS_GROUPS_IDS_KEY)) {
            try {
                mPreviousGroupsIds =
                    (savedInstanceState.getParcelableArray(PREVIOUS_GROUPS_IDS_KEY)
                        ?.map { it as GroupState })?.toMutableList() ?: mutableListOf()
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

        // Observe group
        mGroupViewModel.group.observe(this) {
            val currentGroup = it.group

            mCurrentGroup = currentGroup
            mRecyclingBinIsCurrentGroup = it.isRecycleBin

            if (!currentGroup.isVirtual) {
                // Save group id if real group
                mCurrentGroupState = GroupState(currentGroup.nodeId, it.showFromPosition)

                // Update last access time.
                currentGroup.touch(modified = false, touchParents = false)

                // Add listeners to the add buttons
                addNodeButtonView?.setAddGroupClickListener {
                    GroupEditDialogFragment.create(GroupInfo().apply {
                        if (currentGroup.allowAddNoteInGroup) {
                            notes = ""
                        }
                    }).show(supportFragmentManager, GroupEditDialogFragment.TAG_CREATE_GROUP)
                }
                addNodeButtonView?.setAddEntryClickListener {
                    mDatabase?.let { database ->
                        EntrySelectionHelper.doSpecialAction(intent,
                            {
                                EntryEditActivity.launchToCreate(
                                    this@GroupActivity,
                                    database,
                                    currentGroup.nodeId
                                )
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

            assignGroupViewElements(currentGroup)
            invalidateOptionsMenu()

            loadingView?.hideByFading()
        }

        mGroupViewModel.firstPositionVisible.observe(this) { firstPositionVisible ->
            mCurrentGroupState?.firstVisibleItem = firstPositionVisible
        }

        mGroupEditViewModel.requestIconSelection.observe(this) { iconImage ->
            IconPickerActivity.launch(this@GroupActivity, iconImage)
        }

        mGroupEditViewModel.requestDateTimeSelection.observe(this) { dateInstant ->
            if (dateInstant.type == DateInstant.Type.TIME) {
                // Launch the time picker
                val dateTime = DateTime(dateInstant.date)
                TimePickerFragment.getInstance(dateTime.hourOfDay, dateTime.minuteOfHour)
                    .show(supportFragmentManager, "TimePickerFragment")
            } else {
                // Launch the date picker
                val dateTime = DateTime(dateInstant.date)
                DatePickerFragment.getInstance(
                    dateTime.year,
                    dateTime.monthOfYear - 1,
                    dateTime.dayOfMonth
                )
                    .show(supportFragmentManager, "DatePickerFragment")
            }
        }

        mGroupEditViewModel.onGroupCreated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                mCurrentGroup?.let { currentGroup ->
                    createGroup(currentGroup, groupInfo)
                }
            }
        }

        mGroupEditViewModel.onGroupUpdated.observe(this) { groupInfo ->
            if (groupInfo.title.isNotEmpty()) {
                mOldGroupToUpdate?.let { oldGroupToUpdate ->
                    updateGroup(oldGroupToUpdate, groupInfo)
                }
            }
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        return rootContainerView
    }

    override fun onDatabaseRetrieved(database: Database?) {
        super.onDatabaseRetrieved(database)

        mGroupEditViewModel.setGroupNamesNotAllowed(database?.groupNamesNotAllowed)

        mRecyclingBinEnabled = !mDatabaseReadOnly
                && database?.isRecycleBinEnabled == true

        mRootGroup = database?.rootGroup
        if (mCurrentGroupState == null) {
            mRootGroup?.let { rootGroup ->
                mGroupViewModel.loadGroup(database, rootGroup, 0)
            }
        } else {
            mGroupViewModel.loadGroup(database, mCurrentGroupState)
        }

        // Search suggestion
        database?.let {
            mSearchSuggestionAdapter = SearchEntryCursorAdapter(this, it)
            mOnSuggestionListener = object : SearchView.OnSuggestionListener {
                override fun onSuggestionClick(position: Int): Boolean {
                    mSearchSuggestionAdapter?.let { searchAdapter ->
                        searchAdapter.getEntryFromPosition(position)?.let { entry ->
                            onNodeClick(database, entry)
                        }
                    }
                    return true
                }

                override fun onSuggestionSelect(position: Int): Boolean {
                    return true
                }
            }
        }

        invalidateOptionsMenu()
    }

    override fun onDatabaseActionFinished(
        database: Database,
        actionTask: String,
        result: ActionRunnable.Result
    ) {
        super.onDatabaseActionFinished(database, actionTask, result)

        var newNodes: List<Node> = ArrayList()
        result.data?.getBundle(NEW_NODES_KEY)?.let { newNodesBundle ->
            newNodes = getListNodesFromBundle(database, newNodesBundle)
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
                            try {
                                val entry = newNodes[0] as Entry
                                entrySelectedForKeyboardSelection(database, entry)
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Unable to perform action for keyboard selection after entry update",
                                    e
                                )
                            }
                        },
                        { _, _ ->
                            try {
                                val entry = newNodes[0] as Entry
                                entrySelectedForAutofillSelection(database, entry)
                            } catch (e: Exception) {
                                Log.e(
                                    TAG,
                                    "Unable to perform action for autofill selection after entry update",
                                    e
                                )
                            }
                        },
                        {
                            // Not use
                        }
                    )
                }
            }
        }

        coordinatorLayout?.showActionErrorIfNeeded(result)
        if (!result.isSuccess) {
            reloadCurrentGroup()
        }

        finishNodeAction()

        refreshNumberOfChildren(mCurrentGroup)
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
                mCurrentGroupState = intent.getParcelableExtra(GROUP_STATE_KEY)
                intent.removeExtra(GROUP_STATE_KEY)
            }
            // To transform KEY_SEARCH_INFO in ACTION_SEARCH
            transformSearchInfoIntent(intent)
            if (Intent.ACTION_SEARCH == intent.action) {
                finishNodeAction()
                val searchString =
                    intent.getStringExtra(SearchManager.QUERY)?.trim { it <= ' ' } ?: ""
                mGroupViewModel.loadGroupFromSearch(
                    mDatabase,
                    searchString,
                    PreferencesUtil.omitBackup(this)
                )
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
        outState.putParcelableArray(PREVIOUS_GROUPS_IDS_KEY, mPreviousGroupsIds.toTypedArray())
        mOldGroupToUpdate?.let {
            outState.putParcelable(OLD_GROUP_TO_UPDATE_KEY, it)
        }
        outState.putBoolean(REQUEST_STARTUP_SEARCH_KEY, mRequestStartupSearch)
        super.onSaveInstanceState(outState)
    }

    private fun assignGroupViewElements(group: Group?) {
        // Assign title
        if (group != null) {
            if (groupNameView != null) {
                val title = group.title
                groupNameView?.text = if (title.isNotEmpty()) title else getText(R.string.root)
                groupNameView?.invalidate()
            }
            if (groupMetaView != null) {
                val meta = group.nodeId.toString()
                groupMetaView?.text = meta
                if (meta.isNotEmpty()
                    && !group.isVirtual
                    && PreferencesUtil.showUUID(this)) {
                    groupMetaView?.visibility = View.VISIBLE
                } else {
                    groupMetaView?.visibility = View.GONE
                }
                groupMetaView?.invalidate()
            }
        }

        if (group?.isVirtual == true) {
            searchTitleView?.visibility = View.VISIBLE
            if (toolbar != null) {
                toolbar?.navigationIcon = null
            }
            iconView?.visibility = View.GONE
        } else {
            searchTitleView?.visibility = View.GONE
            // Assign the group icon depending of IconPack or custom icon
            iconView?.visibility = View.VISIBLE
            group?.let { currentGroup ->
                iconView?.let { imageView ->
                    mIconDrawableFactory?.assignDatabaseIcon(
                        imageView,
                        currentGroup.icon,
                        mIconColor
                    )
                }

                if (toolbar != null) {
                    if (group.containsParent())
                        toolbar?.setNavigationIcon(R.drawable.ic_arrow_up_white_24dp)
                    else {
                        toolbar?.navigationIcon = null
                    }
                }
            }
        }

        // Assign number of children
        refreshNumberOfChildren(group)

        // Hide button
        initAddButton(group)
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

    private fun refreshNumberOfChildren(group: Group?) {
        numberChildrenView?.apply {
            if (PreferencesUtil.showNumberEntries(context)) {
                group?.refreshNumberOfChildEntries(Group.ChildFilter.getDefaults(context))
                text = group?.numberOfChildEntries?.toString() ?: ""
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    override fun onScrolled(dy: Int) {
        if (actionNodeMode == null)
            addNodeButtonView?.hideOrShowButtonOnScrollListener(dy)
    }

    override fun onNodeClick(
        database: Database,
        node: Node
    ) {
        when (node.type) {
            Type.GROUP -> try {
                val group = node as Group
                // Save the last not virtual group and it's position
                if (mCurrentGroup?.isVirtual == false) {
                    mCurrentGroupState?.let {
                        mPreviousGroupsIds.add(it)
                    }
                }
                // Open child group
                mGroupViewModel.loadGroup(database, group, 0)

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
                            entryVersioned.nodeId
                        )
                    },
                    {
                        // Nothing here, a search is simply performed
                    },
                    { searchInfo ->
                        if (!database.isReadOnly)
                            entrySelectedForSave(database, entryVersioned, searchInfo)
                        else
                            finish()
                    },
                    { searchInfo ->
                        // Recheck search, only to fix #783 because workflow allows to open multiple search elements
                        SearchHelper.checkAutoSearchInfo(this,
                            database,
                            searchInfo,
                            { openedDatabase, _ ->
                                // Item in search, don't save
                                entrySelectedForKeyboardSelection(openedDatabase, entryVersioned)
                            },
                            {
                                // Item not found, save it if required
                                if (!database.isReadOnly
                                    && searchInfo != null
                                    && PreferencesUtil.isKeyboardSaveSearchInfoEnable(this@GroupActivity)
                                ) {
                                    updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                                }
                                entrySelectedForKeyboardSelection(database, entryVersioned)
                            },
                            {
                                // Normally not append
                                finish()
                            }
                        )
                    },
                    { searchInfo, _ ->
                        if (!database.isReadOnly
                            && searchInfo != null
                            && PreferencesUtil.isAutofillSaveSearchInfoEnable(this@GroupActivity)
                        ) {
                            updateEntryWithSearchInfo(database, entryVersioned, searchInfo)
                        }
                        entrySelectedForAutofillSelection(database, entryVersioned)
                    },
                    { registerInfo ->
                        if (!database.isReadOnly)
                            entrySelectedForRegistration(database, entryVersioned, registerInfo)
                        else
                            finish()
                    })
            } catch (e: ClassCastException) {
                Log.e(TAG, "Node can't be cast in Entry")
            }
        }
    }

    private fun entrySelectedForSave(database: Database, entry: Entry, searchInfo: SearchInfo) {
        reloadCurrentGroup()
        // Save to update the entry
        EntryEditActivity.launchToUpdateForSave(
            this@GroupActivity,
            database,
            entry.nodeId,
            searchInfo
        )
        onLaunchActivitySpecialMode()
    }

    private fun entrySelectedForKeyboardSelection(database: Database, entry: Entry) {
        reloadCurrentGroup()
        // Populate Magikeyboard with entry
        populateKeyboardAndMoveAppToBackground(
            this,
            entry.getEntryInfo(database),
            intent
        )
        onValidateSpecialMode()
    }

    private fun entrySelectedForAutofillSelection(database: Database, entry: Entry) {
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
        database: Database,
        entry: Entry,
        registerInfo: RegisterInfo?
    ) {
        reloadCurrentGroup()
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
        database: Database,
        entry: Entry,
        searchInfo: SearchInfo
    ) {
        val newEntry = Entry(entry)
        newEntry.setEntryInfo(database, newEntry.getEntryInfo(
            database,
            raw = true,
            removeTemplateConfiguration = false
        ).apply {
            saveSearchInfo(database, searchInfo)
        })
        updateEntry(entry, newEntry)
    }

    override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
        // To fix android 4.4 issue
        // https://stackoverflow.com/questions/12436073/datepicker-ondatechangedlistener-called-twice
        if (datePicker?.isShown == true) {
            mGroupEditViewModel.selectDate(year, month, day)
        }
    }

    override fun onTimeSet(view: TimePicker?, hours: Int, minutes: Int) {
        mGroupEditViewModel.selectTime(hours, minutes)
    }

    private fun finishNodeAction() {
        actionNodeMode?.finish()
    }

    override fun onNodeSelected(
        database: Database,
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
        database: Database,
        node: Node
    ): Boolean {
        finishNodeAction()
        onNodeClick(database, node)
        return true
    }

    override fun onEditMenuClick(
        database: Database,
        node: Node
    ): Boolean {
        finishNodeAction()
        when (node.type) {
            Type.GROUP -> {
                mOldGroupToUpdate = node as Group
                GroupEditDialogFragment.update(mOldGroupToUpdate!!.getGroupInfo())
                    .show(
                        supportFragmentManager,
                        GroupEditDialogFragment.TAG_CREATE_GROUP
                    )
            }
            Type.ENTRY -> EntryEditActivity.launchToUpdate(
                this@GroupActivity,
                database,
                (node as Entry).nodeId
            )
        }
        return true
    }

    override fun onCopyMenuClick(
        database: Database,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()

        // Nothing here fragment calls onPasteMenuClick internally
        return true
    }

    override fun onMoveMenuClick(
        database: Database,
        nodes: List<Node>
    ): Boolean {
        actionNodeMode?.invalidate()

        // Nothing here fragment calls onPasteMenuClick internally
        return true
    }

    override fun onPasteMenuClick(
        database: Database,
        pasteMode: GroupFragment.PasteMode?,
        nodes: List<Node>
    ): Boolean {
        when (pasteMode) {
            GroupFragment.PasteMode.PASTE_FROM_COPY -> {
                // Copy
                mCurrentGroup?.let { newParent ->
                    copyNodes(nodes, newParent)
                }
            }
            GroupFragment.PasteMode.PASTE_FROM_MOVE -> {
                // Move
                mCurrentGroup?.let { newParent ->
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
        database: Database,
        nodes: List<Node>
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
        // Refresh suggestions to change preferences
        mSearchSuggestionAdapter?.reInit(this)
        // Padding if lock button visible
        toolbarAction?.updateLockPaddingLeft()
    }

    override fun onPause() {
        super.onPause()

        finishNodeAction()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.search, menu)
        inflater.inflate(R.menu.database, menu)
        if (mDatabaseReadOnly) {
            menu.findItem(R.id.menu_save_database)?.isVisible = false
        }
        if (mSpecialMode == SpecialMode.DEFAULT) {
            MenuUtil.defaultMenuInflater(inflater, menu)
        } else {
            menu.findItem(R.id.menu_reload_database)?.isVisible = false
        }

        // Menu for recycle bin
        if (mRecyclingBinEnabled && mRecyclingBinIsCurrentGroup) {
            inflater.inflate(R.menu.recycle_bin, menu)
        }

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager?

        menu.findItem(R.id.menu_search)?.let {
            val searchView = it.actionView as SearchView?
            searchView?.apply {
                (searchManager?.getSearchableInfo(
                    ComponentName(this@GroupActivity, GroupActivity::class.java)
                ))?.let { searchableInfo ->
                    setSearchableInfo(searchableInfo)
                }
                setIconifiedByDefault(false) // Do not iconify the widget; expand it by default
                suggestionsAdapter = mSearchSuggestionAdapter
                setOnSuggestionListener(mOnSuggestionListener)
            }
            // Expand the search view if defined in settings
            if (mRequestStartupSearch
                && PreferencesUtil.automaticallyFocusSearch(this@GroupActivity)
            ) {
                // To request search only one time
                mRequestStartupSearch = false
                it.expandActionView()
            }
        }

        super.onCreateOptionsMenu(menu)

        // Launch education screen
        Handler(Looper.getMainLooper()).post {
            performedNextEducation(
                GroupActivityEducation(this),
                menu
            )
        }

        return true
    }

    private fun performedNextEducation(
        groupActivityEducation: GroupActivityEducation,
        menu: Menu
    ) {

        // If no node, show education to add new one
        val addNodeButtonEducationPerformed = actionNodeMode == null
                && addNodeButtonView?.addButtonView != null
                && addNodeButtonView!!.isEnable
                && groupActivityEducation.checkAndPerformedAddNodeButtonEducation(
            addNodeButtonView?.addButtonView!!,
            {
                addNodeButtonView?.openButtonIfClose()
            },
            {
                performedNextEducation(groupActivityEducation, menu)
            }
        )
        if (!addNodeButtonEducationPerformed) {

            val searchMenuEducationPerformed = toolbar != null
                    && toolbar!!.findViewById<View>(R.id.menu_search) != null
                    && groupActivityEducation.checkAndPerformedSearchMenuEducation(
                toolbar!!.findViewById(R.id.menu_search),
                {
                    menu.findItem(R.id.menu_search).expandActionView()
                },
                {
                    performedNextEducation(groupActivityEducation, menu)
                })

            if (!searchMenuEducationPerformed) {

                val sortMenuEducationPerformed = toolbar != null
                        && toolbar!!.findViewById<View>(R.id.menu_sort) != null
                        && groupActivityEducation.checkAndPerformedSortMenuEducation(
                    toolbar!!.findViewById(R.id.menu_sort),
                    {
                        onOptionsItemSelected(menu.findItem(R.id.menu_sort))
                    },
                    {
                        performedNextEducation(groupActivityEducation, menu)
                    })

                if (!sortMenuEducationPerformed) {
                    // lockMenuEducationPerformed
                    val lockButtonView = findViewById<View>(R.id.lock_button_icon)
                    lockButtonView != null
                            && groupActivityEducation.checkAndPerformedLockMenuEducation(
                        lockButtonView,
                        {
                            lockAndExit()
                        },
                        {
                            performedNextEducation(groupActivityEducation, menu)
                        })
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_search ->
                //onSearchRequested();
                return true
            R.id.menu_save_database -> {
                saveDatabase()
                return true
            }
            R.id.menu_reload_database -> {
                reloadDatabase()
                return true
            }
            R.id.menu_empty_recycle_bin -> {
                if (mRecyclingBinEnabled && mRecyclingBinIsCurrentGroup) {
                    mCurrentGroup?.getChildren()?.let { listChildren ->
                        // Automatically delete all elements
                        deleteNodes(listChildren, true)
                        finishNodeAction()
                    }
                }
                return true
            }
            else -> {
                // Check the time lock before launching settings
                MenuUtil.onDefaultMenuOptionsItemSelected(this, item, true)
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

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        /*
         * ACTION_SEARCH automatically forces a new task. This occurs when you open a kdb file in
         * another app such as Files or GoogleDrive and then Search for an entry. Here we remove the
         * FLAG_ACTIVITY_NEW_TASK flag bit allowing search to open it's activity in the current task.
         */
        if (Intent.ACTION_SEARCH == intent.action) {
            var flags = intent.flags
            flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
            intent.flags = flags
        }

        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // To create tree dialog for icon
        IconPickerActivity.onActivityResult(requestCode, resultCode, data) { icon ->
            mGroupEditViewModel.selectIcon(icon)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillHelper.onActivityResultSetResultAndFinish(this, requestCode, resultCode, data)
        }

        // Directly used the onActivityResult in fragment
        mGroupFragment?.onActivityResult(requestCode, resultCode, data)
    }

    private fun removeSearch() {
        intent.removeExtra(AUTO_SEARCH_KEY)
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
        }
    }

    private fun reloadCurrentGroup() {
        // Remove search in intent
        removeSearch()
        // Reload real group
        try {
            mGroupViewModel.loadGroup(mDatabase, mCurrentGroupState)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to rebuild the list after deletion", e)
        }
    }

    override fun onBackPressed() {
        if (mGroupFragment?.nodeActionSelectionMode == true) {
            finishNodeAction()
        } else {
            // Normal way when we are not in root
            if (mRootGroup != null && mRootGroup != mCurrentGroup) {
                when {
                    Intent.ACTION_SEARCH == intent.action -> {
                        // Remove the search
                        reloadCurrentGroup()
                    }
                    mPreviousGroupsIds.isEmpty() -> {
                        super.onRegularBackPressed()
                    }
                    else -> {
                        // Load the previous group
                        mGroupViewModel.loadGroup(mDatabase, mPreviousGroupsIds.removeLast())
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
                    super.onRegularBackPressed()
                } else {
                    backToTheAppCaller()
                }
            }
        }
    }

    data class GroupState(
        var groupId: NodeId<*>?,
        var firstVisibleItem: Int?
    ) : Parcelable {

        private constructor(parcel: Parcel) : this(
            parcel.readParcelable<NodeId<*>>(NodeId::class.java.classLoader),
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
                   database: Database,
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
                                  database: Database,
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
                                database: Database,
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
                                             database: Database,
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
        fun launchForAutofillResult(activity: Activity,
                                    database: Database,
                                    autofillComponent: AutofillComponent,
                                    searchInfo: SearchInfo? = null,
                                    autoSearch: Boolean = false) {
            if (database.loaded) {
                checkTimeAndBuildIntent(activity, null) { intent ->
                    intent.putExtra(AUTO_SEARCH_KEY, autoSearch)
                    AutofillHelper.startActivityForAutofillResult(
                        activity,
                        intent,
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
                                  database: Database,
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
        fun launch(activity: Activity,
                   database: Database,
                   onValidateSpecialMode: () -> Unit,
                   onCancelSpecialMode: () -> Unit,
                   onLaunchActivitySpecialMode: () -> Unit) {
            EntrySelectionHelper.doSpecialAction(activity.intent,
                    {
                        GroupActivity.launch(
                            activity,
                            database,
                            true
                        )
                    },
                    { searchInfo ->
                        SearchHelper.checkAutoSearchInfo(activity,
                                database,
                                searchInfo,
                                { _, _ ->
                                    // Response is build
                                    GroupActivity.launchForSearchResult(activity,
                                        database,
                                        searchInfo,
                                        true)
                                    onLaunchActivitySpecialMode()
                                },
                                {
                                    // Here no search info found
                                    if (database.isReadOnly) {
                                        GroupActivity.launchForSearchResult(activity,
                                            database,
                                            searchInfo,
                                            false)
                                    } else {
                                        GroupActivity.launchForSaveResult(activity,
                                            database,
                                            searchInfo,
                                            false)
                                    }
                                    onLaunchActivitySpecialMode()
                                },
                                {
                                    // Simply close if database not opened, normally not happened
                                    onCancelSpecialMode()
                                }
                        )
                    },
                    { searchInfo ->
                        // Save info used with OTP
                        if (database.loaded) {
                            if (!database.isReadOnly) {
                                GroupActivity.launchForSaveResult(
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
                        SearchHelper.checkAutoSearchInfo(activity,
                                database,
                                searchInfo,
                                { _, items ->
                                    // Response is build
                                    if (items.size == 1) {
                                        populateKeyboardAndMoveAppToBackground(activity,
                                                items[0],
                                                activity.intent)
                                        onValidateSpecialMode()
                                    } else {
                                        // Select the one we want
                                        GroupActivity.launchForKeyboardSelectionResult(activity,
                                            database,
                                            searchInfo,
                                            true)
                                        onLaunchActivitySpecialMode()
                                    }
                                },
                                {
                                    // Here no search info found, disable auto search
                                    GroupActivity.launchForKeyboardSelectionResult(activity,
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
                                        GroupActivity.launchForAutofillResult(activity,
                                            database,
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
                        if (!database.isReadOnly) {
                            SearchHelper.checkAutoSearchInfo(activity,
                                    database,
                                    registerInfo?.searchInfo,
                                    { _, _ ->
                                        // No auto search, it's a registration
                                        GroupActivity.launchForRegistration(activity,
                                            database,
                                            registerInfo)
                                        onLaunchActivitySpecialMode()
                                    },
                                    {
                                        // Here no search info found, disable auto search
                                        GroupActivity.launchForRegistration(activity,
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
