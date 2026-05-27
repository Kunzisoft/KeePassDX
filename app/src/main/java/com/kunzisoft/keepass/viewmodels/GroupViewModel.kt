/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.app.SearchManager
import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.GroupActivity.Companion.retrieveAutoSearch
import com.kunzisoft.keepass.activities.GroupActivity.SearchState
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.DefaultNodeFilter
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
import com.kunzisoft.keepass.model.SearchGroupInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.model.SortedGroupInfo
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * ViewModel for managing groups.
 */
class GroupViewModel(application: Application): AndroidViewModel(application) {

    private var mDatabase: ContextualDatabase? = null

    // Manage Recycle Bin

    private var mRecycleBinAllowed = false
    private var mRecyclingBinIsCurrentGroup = false

    private var mAddGroupOrEntryAllowed = false
    private val mAddNodeButtonAllowed: Boolean
        get() = mAddGroupOrEntryAllowed && !actionNodeInProgress()

    // Main group currently in memory
    var mainGroup: GroupInfo? = null
        private set

    // Group currently visible (search or main group)
    var currentGroup: GroupInfo? = null
        private set

    var children: List<SortedNodeInfo>? = null
        private set

    // Group state to retrieve position, not a search
    private var mMainGroupState: GroupState? = null
    // Previous groups states to retrieve history and position
    private var mPreviousGroupsStates = mutableListOf<GroupState>()

    /*
     * Manage search state
     */
    var mSearchState: SearchState? = null
    // To mainly manage keyboard
    var mAutoSearch: Boolean = false
    // To manage temp search
    var mTempSearchInfo: Boolean = false
    var mLockSearchListeners = false
    private var mRequestStartupSearch = true

    /*
     * Manage action in group
     */
    var nodeActionSelectionMode = false
        private set
    var nodeActionPasteMode: PasteMode = PasteMode.UNDEFINED
        private set
    private val listActionNodes = mutableListOf<SortedNodeInfo>()
    private val listPasteNodes = mutableListOf<SortedNodeInfo>()
    var actionNodeMode: ActionMode? = null

    private val _groupUIState = MutableStateFlow<GroupUISTate>(GroupUISTate())
    val groupUIState: StateFlow<GroupUISTate> = _groupUIState.asStateFlow()

    private val _actionsNodes = MutableStateFlow<List<SortedNodeInfo>>(listOf())
    val actionsNodes: StateFlow<List<SortedNodeInfo>> = _actionsNodes.asStateFlow()

    private val _removeNodeAction = MutableSharedFlow<Unit>(replay = 0)
    val removeNodeAction: SharedFlow<Unit> = _removeNodeAction.asSharedFlow()

    private val _requestOpenNode = MutableSharedFlow<SortedNodeInfo>(replay = 0)
    val requestOpenNode: SharedFlow<SortedNodeInfo> = _requestOpenNode.asSharedFlow()

    private val _requestEditNode = MutableSharedFlow<NodeInfo>(replay = 0)
    val requestEditNode: SharedFlow<NodeInfo> = _requestEditNode.asSharedFlow()

    private val _requestCopyNodes = MutableSharedFlow<List<NodeInfo>>(replay = 0)
    val requestCopyNodes: SharedFlow<List<NodeInfo>> = _requestCopyNodes.asSharedFlow()

    private val _requestMoveNodes = MutableSharedFlow<List<NodeInfo>>(replay = 0)
    val requestMoveNodes: SharedFlow<List<NodeInfo>> = _requestMoveNodes.asSharedFlow()

    private val _requestDeleteNodes = MutableSharedFlow<List<NodeInfo>>(replay = 0)
    val requestDeleteNodes: SharedFlow<List<NodeInfo>> = _requestDeleteNodes.asSharedFlow()

    private val _requestPaste = MutableSharedFlow<PasteAction>(replay = 0)
    val requestPasteNodes: SharedFlow<PasteAction> = _requestPaste.asSharedFlow()

    private val _provideNodeActionCallback = MutableSharedFlow<ActionMode.Callback>(replay = 0)
    val provideNodeActionCallback: SharedFlow<ActionMode.Callback> = _provideNodeActionCallback.asSharedFlow()

    private val _requestAddSearch = MutableSharedFlow<Unit>(replay = 0)
    val addSearch: SharedFlow<Unit> = _requestAddSearch.asSharedFlow()

    private val _removeSearch = MutableSharedFlow<Unit>(replay = 0)
    val removeSearch: SharedFlow<Unit> = _removeSearch.asSharedFlow()

    private val _showKeyboard = MutableSharedFlow<Boolean>(replay = 0)
    val showKeyboard: SharedFlow<Boolean> = _showKeyboard.asSharedFlow()

    private val _showPosition = MutableSharedFlow<Int>(replay = 0)
    val showPosition: SharedFlow<Int> = _showPosition.asSharedFlow()

    private val _scrollTo = MutableSharedFlow<Int>(replay = 0)
    val scrollTo: SharedFlow<Int> = _scrollTo.asSharedFlow()

    private var mDefaultSearchParameters: SearchParameters = PreferencesUtil.getDefaultSearchParameters(getApplication())
    private var mAutoFocusSearch: Boolean = PreferencesUtil.automaticallyFocusSearch(getApplication())
    private var mRecursiveNumberEntries: Boolean = PreferencesUtil.recursiveNumberEntries(getApplication())
    private var mShowExpiredEntries: Boolean = PreferencesUtil.showExpiredEntries(getApplication())
    private var mShowTemplates: Boolean = PreferencesUtil.showTemplates(getApplication())
    private var mNodeFilter: NodeFilter = EmptyNodeFilter()

    fun onDatabaseLoaded(
        database: ContextualDatabase,
        recycleBinAllowed: Boolean
    ) {
        this.mDatabase = database
        this.mRecycleBinAllowed = recycleBinAllowed
        this.mNodeFilter = DefaultNodeFilter(
            database = mDatabase,
            showExpired = mShowExpiredEntries,
            showTemplates = mShowTemplates
        )
    }

    fun loadGroup() {
        if (mSearchState != null) {
            finishNodeAction()
            loadSearch(mDatabase)
        } else loadMainGroup(mDatabase)
    }

    fun loadMainGroup(
        database: ContextualDatabase?,
        groupState: GroupState? = mMainGroupState
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _groupUIState.update { groupState ->
                    groupState.copy(loaded = false)
                }
                val groupId = groupState?.groupId
                val showFromPosition = groupState?.firstVisibleItem
                val group = if (groupId != null) {
                    database?.getGroupInfoById(groupId)
                } else {
                    database?.getRootGroupInfo()
                }
                if (group != null) {
                    mRecyclingBinIsCurrentGroup = group.isRecycleBin(database)
                    mainGroup = group
                    // Save group state
                    mMainGroupState = GroupState(group.nodeId, showFromPosition)
                    currentGroup = group
                    mSearchState = null
                    // Breadcrumbs
                    val breadcrumbs = mDatabase?.getBreadcrumbsFrom(
                        groupId = group.nodeId,
                        recursiveNumberOfEntries = mRecursiveNumberEntries,
                        nodeFilter = mNodeFilter
                    ) ?: listOf()
                    // Children
                    children = mDatabase?.getSortedChildrenOf(
                        parentId = group.nodeId,
                        recursiveNumberOfEntries = mRecursiveNumberEntries,
                        nodeFilter = mNodeFilter
                    ) ?: listOf()
                    // To enable add button
                    val addGroupEnabled = mDatabase?.allowAddGroupIn(group = group) ?: false
                    val addEntryEnabled = mDatabase?.allowAddEntryIn(group = group) ?: false
                    mAddGroupOrEntryAllowed = addGroupEnabled || addEntryEnabled
                    withContext(Dispatchers.Main) {
                        _groupUIState.update { groupState ->
                            groupState.copy(
                                loaded = true,
                                breadcrumbs = breadcrumbs,
                                group = group,
                                children = children,
                                showAddGroupButton = addGroupEnabled,
                                showAddEntryButton = addEntryEnabled,
                                showAddNodeButton = mAddNodeButtonAllowed
                            )
                        }
                        showFromPosition?.let {
                            _showPosition.emit(showFromPosition)
                        }
                    }
                }
            }
        }
    }

    fun loadChildGroup(
        database: ContextualDatabase?,
        groupId: NodeId<*>?
    ) {
        // Save the last not virtual group and it's position
        if (currentGroup !is SearchGroupInfo) {
            mMainGroupState?.let {
                mPreviousGroupsStates.add(it)
            }
        }
        loadMainGroup(database, GroupState(groupId, firstVisibleItem = 0))
    }

    fun loadSearch(
        database: ContextualDatabase?,
        searchState: SearchState? = mSearchState,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                searchState?.let {
                    withContext(Dispatchers.Main) {
                        _groupUIState.update { groupState ->
                            groupState.copy(loaded = false)
                        }
                    }
                    val searchParameters = searchState.searchParameters
                    val showFromPosition = searchState.firstVisibleItem
                    val group = database?.createSearchGroupInfo(
                        searchParameters = searchParameters,
                        fromGroup = mMainGroupState?.groupId,
                        max = SearchHelper.MAX_SEARCH_ENTRY
                    )
                    if (group != null) {
                        currentGroup = group
                        mSearchState = searchState
                        val children = group.getSearchResults()
                        withContext(Dispatchers.Main) {
                            _groupUIState.update { groupState ->
                                groupState.copy(
                                    loaded = true,
                                    group = group,
                                    children = children,
                                    showAddNodeButton = false
                                )
                            }
                            showFromPosition?.let {
                                _showPosition.emit(showFromPosition)
                            }
                        }
                    }
                } ?: Log.e(TAG, "Search state is null")
            }
        }
    }

    fun manageIntent(intent: Intent?) {
        intent?.let {
            // To get the form filling search as temp search
            val searchInfo: SearchInfo? = intent.retrieveSearchInfo()
            val autoSearch = intent.retrieveAutoSearch()
            // Get search query
            if (searchInfo != null && autoSearch) {
                mAutoSearch = true
                mTempSearchInfo = true
                searchInfo.getSearchParametersFromSearchInfo(getApplication()) {
                    mSearchState = SearchState(
                        searchParameters = it,
                        firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
                    )
                }
            } else if (intent.action == Intent.ACTION_SEARCH) {
                mAutoSearch = true
                mSearchState = SearchState(
                    searchParameters = mDefaultSearchParameters.apply {
                        searchQuery = intent.getStringExtra(SearchManager.QUERY)
                            ?.trim { it <= ' ' } ?: ""
                    },
                    firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
                )
            } else if (mRequestStartupSearch && mAutoFocusSearch) {
                // Expand the search view if defined in settings
                // To request search only one time
                mRequestStartupSearch = false
                viewModelScope.launch {
                    _requestAddSearch.emit(Unit)
                }
            }
            intent.action = Intent.ACTION_DEFAULT
            intent.removeExtra(SearchManager.QUERY)
        }
    }

    fun searchText(database: ContextualDatabase?, text: String?) {
        if (text != null && !mLockSearchListeners) {
            mSearchState?.let { searchState ->
                searchState.searchParameters.searchQuery = text
                loadSearch(database, searchState)
            }
        }
    }

    fun searchWithParameters(database: ContextualDatabase?, searchParameters: SearchParameters) {
        mSearchState?.let { searchState ->
            searchParameters.searchQuery = searchState.searchParameters.searchQuery
            searchState.searchParameters = searchParameters
            loadSearch(database, searchState)
        }
    }

    fun assignSearchParameters(searchParameters: SearchParameters?) {
        if (mSearchState == null) {
            mSearchState = SearchState(
                searchParameters ?: mDefaultSearchParameters,
                firstVisibleItem = 0
            )
        }
    }

    fun assignPosition(position: Int) {
        mSearchState?.firstVisibleItem = position
        mMainGroupState?.firstVisibleItem = position
    }

    /*
     * Actions
     */

    fun performNodeClick(
        database: ContextualDatabase,
        node: SortedNodeInfo
    ) {
        viewModelScope.launch {
            if (nodeActionSelectionMode) {
                if (listActionNodes.contains(node)) {
                    // Remove selected item if already selected
                    listActionNodes.remove(node)
                } else {
                    // Add selected item if not already selected
                    listActionNodes.add(node)
                }
                selectNodes(database, listActionNodes)
                _actionsNodes.value = listActionNodes.toList()
            } else {
                _requestOpenNode.emit(node)
            }
        }
    }

    fun performLongNodeClick(
        database: ContextualDatabase,
        node: SortedNodeInfo
    ): Boolean {
        viewModelScope.launch {
            if (nodeActionPasteMode == PasteMode.UNDEFINED) {
                // Select the first item after a long click
                if (!listActionNodes.contains(node))
                    listActionNodes.add(node)
                selectNodes(database, listActionNodes)
                _actionsNodes.value = listActionNodes.toList()
                _showKeyboard.emit(false)
            }
        }
        return true
    }

    private fun containsRecycleBin(
        database: ContextualDatabase?,
        nodes: List<NodeInfo>
    ): Boolean {
        return nodes.any { it is GroupInfo && it.isRecycleBin(database) }
    }

    private fun actionNodesCallback(
        database: ContextualDatabase,
        nodes: List<NodeInfo>
    ) : ActionMode.Callback {

        return object : ActionMode.Callback {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                _groupUIState.update { groupState ->
                    groupState.copy(showAddNodeButton = false)
                }
                nodeActionSelectionMode = false
                nodeActionPasteMode = PasteMode.UNDEFINED
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.clear()
                if (nodeActionPasteMode != PasteMode.UNDEFINED) {
                    mode?.menuInflater?.inflate(R.menu.node_paste_menu, menu)
                } else {
                    nodeActionSelectionMode = true
                    mode?.menuInflater?.inflate(R.menu.node_menu, menu)

                    // Open and Edit for a single item
                    if (nodes.size == 1) {
                        // Edition
                        if (database.isReadOnly || containsRecycleBin(database, nodes)) {
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
                    if (database.isReadOnly || containsRecycleBin(database, nodes)) {
                        menu?.removeItem(R.id.menu_delete)
                    }
                }

                // Add the number of items selected in title
                mode?.title = nodes.size.toString()
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                val node = nodes[0]
                return when (item?.itemId) {
                    R.id.menu_open -> {
                        finishNodeAction()
                        viewModelScope.launch {
                            // TODO prevent cast
                            _requestOpenNode.emit(node as SortedNodeInfo)
                        }
                        true
                    }
                    R.id.menu_edit -> {
                        finishNodeAction()
                        viewModelScope.launch {
                            _requestEditNode.emit(node)
                        }
                        true
                    }
                    R.id.menu_copy -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_COPY
                        //_removeNodeAction.emit(Unit)
                        invalidateNodeAction()
                        viewModelScope.launch {
                            _requestCopyNodes.emit(nodes)
                        }
                        nodeActionSelectionMode = false
                        true
                    }
                    R.id.menu_move -> {
                        nodeActionPasteMode = PasteMode.PASTE_FROM_MOVE
                        //_removeNodeAction.emit(Unit)
                        invalidateNodeAction()
                        viewModelScope.launch {
                            _requestMoveNodes.emit(nodes)
                        }
                        nodeActionSelectionMode = false
                        true
                    }
                    R.id.menu_delete -> {
                        viewModelScope.launch {
                            _requestDeleteNodes.emit(nodes)
                        }
                        finishNodeAction()
                        true
                    }
                    R.id.menu_paste -> {
                        mainGroup?.nodeId?.let { parentId ->
                            viewModelScope.launch {
                                _requestPaste.emit(
                                    PasteAction(
                                        nodeActionPasteMode,
                                        parentId,
                                        nodes
                                    )
                                )
                            }
                            finishNodeAction()
                            nodeActionPasteMode = PasteMode.UNDEFINED
                            nodeActionSelectionMode = false
                        }
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                viewModelScope.launch {
                    _removeNodeAction.emit(Unit)
                }
                clearNodeAction()
                _groupUIState.update { groupState ->
                    groupState.copy(showAddNodeButton = mAddNodeButtonAllowed)
                }
            }
        }
    }

    fun selectNodes(
        database: ContextualDatabase,
        nodes: List<SortedNodeInfo>
    ): Boolean {
        if (nodes.isNotEmpty()) {
            if (!actionNodeInProgress()) {
                viewModelScope.launch {
                    _provideNodeActionCallback.emit(
                        actionNodesCallback(
                            database,
                            nodes as List<NodeInfo> // TODO Prevent cast
                        )
                    )
                }
            } else {
                invalidateNodeAction()
            }
        } else {
            finishNodeAction()
        }
        return true
    }

    fun recycleBinActionsAllowed(): Boolean = mRecycleBinAllowed && mRecyclingBinIsCurrentGroup

    fun actionNodeInProgress(): Boolean {
        return actionNodeMode != null
    }

    fun setNodeAction(actionNodeMode: ActionMode?) {
        this.actionNodeMode = actionNodeMode
    }

    fun invalidateNodeAction() {
        actionNodeMode?.invalidate()
    }

    fun finishNodeAction() {
        actionNodeMode?.finish()
    }

    fun clearNodeAction() {
        listActionNodes.clear()
        listPasteNodes.clear()
        actionNodeMode = null
        nodeActionPasteMode = PasteMode.UNDEFINED
        nodeActionSelectionMode = false
    }

    fun isCurrentGroupRoot(database: ContextualDatabase?): Boolean {
        return currentGroup?.isRoot(database) == true
    }

    fun previousGroupExists(): Boolean = mPreviousGroupsStates.isNotEmpty()

    fun loadPreviousGroup(database: ContextualDatabase?) {
        if (previousGroupExists()) {
            return loadMainGroup(
                database = database,
                groupState = mPreviousGroupsStates.removeAt(mPreviousGroupsStates.lastIndex)
            )
        }
    }

    fun scrollTo(dy: Int) {
        viewModelScope.launch {
            if (!actionNodeInProgress())
                _scrollTo.emit(dy)
        }
    }

    fun clearSearch() {
        mSearchState = null
        mTempSearchInfo = false
    }

    data class GroupUISTate(
        val loaded: Boolean = false,
        val breadcrumbs: List<SortedGroupInfo>? = null,
        val group: GroupInfo? = null,
        val children: List<SortedNodeInfo>? = null,
        val showAddNodeButton: Boolean = false,
        val showAddGroupButton: Boolean = false,
        val showAddEntryButton: Boolean = false
    )

    data class GroupState(
        var groupId: NodeId<*>?,
        var firstVisibleItem: Int?
    )

    data class PasteAction(
        val pasteMode: PasteMode,
        val parentId: NodeId<*>,
        val nodes: List<NodeInfo>
    )

    enum class PasteMode {
        UNDEFINED, PASTE_FROM_COPY, PASTE_FROM_MOVE
    }

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}