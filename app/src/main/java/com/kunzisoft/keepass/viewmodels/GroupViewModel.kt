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
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.activities.GroupActivity.SearchState
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.DefaultNodeFilter
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.element.node.Nodes
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.helper.SearchHelper.getSearchParametersFromSearchInfo
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.model.SortedEntryInfo
import com.kunzisoft.keepass.model.SortedGroupInfo
import com.kunzisoft.keepass.model.SortedNodeInfo
import com.kunzisoft.keepass.model.toNodes
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    var isCurrentGroupIsRoot: Boolean = false
    var isCurrentGroupIsSearch: Boolean = false

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
    private val _nodeActionState = MutableStateFlow(NodeActionState())
    val nodeActionState: StateFlow<NodeActionState> = _nodeActionState.asStateFlow()

    // Derived StateFlow from nodeActionState.selectedNodes to keep sync
    val actionsNodes: StateFlow<List<SortedNodeInfo>> = nodeActionState
        .map { it.selectedNodes }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    val nodeActionSelectionMode: Boolean
        get() = _nodeActionState.value.selectionMode

    private val _groupUIState = MutableStateFlow<GroupUISTate>(GroupUISTate())
    val groupUIState: StateFlow<GroupUISTate> = _groupUIState.asStateFlow()

    private val _removeNodeAction = MutableSharedFlow<Unit>(replay = 0)
    val removeNodeAction: SharedFlow<Unit> = _removeNodeAction.asSharedFlow()

    private val _requestOpenGroup = MutableSharedFlow<GroupId>(replay = 0)
    val requestOpenGroup: SharedFlow<GroupId> = _requestOpenGroup.asSharedFlow()

    private val _requestOpenEntry = MutableSharedFlow<EntryInfo>(replay = 0)
    val requestOpenEntry: SharedFlow<EntryInfo> = _requestOpenEntry.asSharedFlow()

    private val _requestEditGroup = MutableSharedFlow<GroupInfo>(replay = 0)
    val requestEditGroup: SharedFlow<GroupInfo> = _requestEditGroup.asSharedFlow()

    private val _requestEditEntry = MutableSharedFlow<EntryInfo>(replay = 0)
    val requestEditEntry: SharedFlow<EntryInfo> = _requestEditEntry.asSharedFlow()

    private val _requestCopyNodes = MutableSharedFlow<Nodes>(replay = 0)
    val requestCopyNodes: SharedFlow<Nodes> = _requestCopyNodes.asSharedFlow()

    private val _requestMoveNodes = MutableSharedFlow<Nodes>(replay = 0)
    val requestMoveNodes: SharedFlow<Nodes> = _requestMoveNodes.asSharedFlow()

    private val _requestDeleteNodes = MutableSharedFlow<DeleteActionState>(replay = 0)
    val requestDeleteNodes: SharedFlow<DeleteActionState> = _requestDeleteNodes.asSharedFlow()

    private val _requestPaste = MutableSharedFlow<PasteActionState>(replay = 0)
    val requestPasteNodes: SharedFlow<PasteActionState> = _requestPaste.asSharedFlow()

    private val _requestShowGroup = MutableSharedFlow<GroupInfo>(replay = 0)
    val requestShowGroup: SharedFlow<GroupInfo> = _requestShowGroup.asSharedFlow()

    private val _hideKeyboard = MutableSharedFlow<Unit>(replay = 0)
    val hideKeyboard: SharedFlow<Unit> = _hideKeyboard.asSharedFlow()

    private val _showPosition = MutableSharedFlow<Int>(replay = 0)
    val showPosition: SharedFlow<Int> = _showPosition.asSharedFlow()

    private val _scrollTo = MutableSharedFlow<Int>(replay = 0)
    val scrollTo: SharedFlow<Int> = _scrollTo.asSharedFlow()

    private val _onSortSelected = MutableSharedFlow<SortNode>(replay = 0)
    val onSortSelected: SharedFlow<SortNode> = _onSortSelected.asSharedFlow()

    private var mDefaultSearchParameters: SearchParameters = SearchParameters()
    private var mAutoFocusSearch: Boolean = false
    private var mRecursiveNumberEntries: Boolean = false
    private var mShowExpiredEntries: Boolean = false
    private var mShowTemplates: Boolean = false
    private var mNodeFilter: NodeFilter = EmptyNodeFilter()

    fun assignPreferences() {
        mDefaultSearchParameters = PreferencesUtil.getDefaultSearchParameters(getApplication())
        mAutoFocusSearch = PreferencesUtil.automaticallyFocusSearch(getApplication())
        mRecursiveNumberEntries = PreferencesUtil.recursiveNumberEntries(getApplication())
        mShowExpiredEntries = PreferencesUtil.showExpiredEntries(getApplication())
        mShowTemplates = PreferencesUtil.showTemplates(getApplication())
    }

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
            loadSearch(mDatabase)
        } else loadMainGroup(mDatabase)
    }

    private fun loadMainGroup(
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
                    isCurrentGroupIsRoot = group.isRoot(mDatabase) == true
                    isCurrentGroupIsSearch = false
                    mSearchState = null
                    // Breadcrumbs
                    val breadcrumbs = mDatabase?.getBreadcrumbsFrom(
                        groupId = group.nodeId,
                        recursiveNumberOfEntries = mRecursiveNumberEntries,
                        nodeFilter = mNodeFilter
                    ) ?: listOf()
                    // Children
                    val children = mDatabase?.getSortedChildrenOf(
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

    fun loadGroup(
        database: ContextualDatabase?,
        groupId: GroupId?
    ) {
        // Save the last not virtual group and it's position
        if (!isCurrentGroupIsSearch) {
            mMainGroupState?.let {
                mPreviousGroupsStates.add(it)
            }
        }
        loadMainGroup(database, GroupState(groupId, firstVisibleItem = 0))
    }

    private fun loadSearch(
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
                        isCurrentGroupIsRoot = false
                        isCurrentGroupIsSearch = true
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

    fun processSearchData(
        searchInfo: SearchInfo?,
        searchQuery: String?
    ) {
        // Get search query
        if (searchInfo != null) {
            mAutoSearch = true
            mTempSearchInfo = true
            searchInfo.getSearchParametersFromSearchInfo(getApplication()) {
                mSearchState = SearchState(
                    searchParameters = it,
                    firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
                )
            }
        } else if (searchQuery != null) {
            mAutoSearch = true
            mSearchState = SearchState(
                searchParameters = mDefaultSearchParameters.apply {
                    this.searchQuery = searchQuery.trim { it <= ' ' }
                },
                firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
            )
        } else if (mRequestStartupSearch && mAutoFocusSearch) {
            // Expand the search view if defined in settings
            // To request search only one time
            mRequestStartupSearch = false
            mSearchState = SearchState(
                searchParameters = mDefaultSearchParameters,
                firstVisibleItem = mSearchState?.firstVisibleItem ?: 0
            )
        }
    }

    fun onSortSelected(
        sortNode: SortNodeEnum? = null,
        sortNodeParameters: SortNodeEnum.SortNodeParameters? = null
    ) {
        PreferencesUtil.saveNodeSort(
            context = getApplication(),
            sortNodeEnum = sortNode,
            sortNodeParameters = sortNodeParameters
        )
        viewModelScope.launch {
            val sortDatabaseParameters = mDatabase?.let { database ->
                SortNodeEnum.SortDatabaseParameters(
                    recycleBinEnabled = database.isRecycleBinEnabled,
                    recycleBinId = database.recycleBin?.nodeId
                )
            }
            _onSortSelected.emit(SortNode(
                sortNodeEnum = sortNode,
                sortNodeParameters = sortNodeParameters,
                sortDatabaseParameters = sortDatabaseParameters
            ))
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
     * Breadcrumbs
     */

    fun onBreadcrumbClicked(group: SortedGroupInfo) {
        viewModelScope.launch {
            // If last item & not a virtual root group
            val currentGroup = mainGroup
            if (currentGroup != null && group.nodeId == currentGroup.nodeId
                && (currentGroup.nodeId != mDatabase?.rootGroup?.nodeId
                        || mDatabase?.rootGroupIsVirtual != true)
            ) {
                finishNodeAction()
                _requestShowGroup.emit(currentGroup)
            } else {
                if (nodeActionSelectionMode) {
                    finishNodeAction()
                }
                _requestOpenGroup.emit(group.nodeId)
            }
        }
    }

    fun onBreadcrumbLongClicked(group: SortedGroupInfo) {
        viewModelScope.launch {
            val currentGroup = mainGroup
            if (currentGroup != null && group.nodeId == currentGroup.nodeId
                && (currentGroup.nodeId != mDatabase?.rootGroup?.nodeId
                        || mDatabase?.rootGroupIsVirtual != true)
            ) {
                finishNodeAction()
                _requestEditGroup.emit(currentGroup)
            } else {
                onBreadcrumbClicked(group)
            }
        }
    }

    /*
     * Actions
     */

    fun performNodeClick(node: SortedNodeInfo) {
        viewModelScope.launch {
            val state = _nodeActionState.value
            if (state.selectionMode) {
                val selectedNodes = state.selectedNodes.toMutableList()
                val existingNode = selectedNodes.find { it.nodeId == node.nodeId }
                if (existingNode != null) {
                    // Remove selected item if already selected
                    selectedNodes.remove(existingNode)
                } else {
                    // Add selected item if not already selected
                    selectedNodes.add(node)
                }
                selectNodes(selectedNodes)
            } else {
                when (node) {
                    is SortedGroupInfo -> _requestOpenGroup.emit(node.nodeId)
                    is SortedEntryInfo -> _requestOpenEntry.emit(node)
                }
            }
        }
    }

    fun performLongNodeClick(node: SortedNodeInfo): Boolean {
        viewModelScope.launch {
            if (_nodeActionState.value.pasteMode == PasteMode.UNDEFINED) {
                // Select the first item after a long click
                val selectedNodes = _nodeActionState.value.selectedNodes.toMutableList()
                if (selectedNodes.none { it.nodeId == node.nodeId })
                    selectedNodes.add(node)
                selectNodes(selectedNodes)
                _hideKeyboard.emit(Unit)
            }
        }
        return true
    }

    fun selectNodes(nodes: List<SortedNodeInfo>): Boolean {
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    isActive = true,
                    selectionMode = it.pasteMode == PasteMode.UNDEFINED,
                    selectedNodes = nodes
                )
            }
        } else {
            finishNodeAction()
        }
        return true
    }

    fun onOpenNodeClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            finishNodeAction()
            viewModelScope.launch {
                when (val node = nodes[0]) {
                    is SortedGroupInfo -> _requestOpenGroup.emit(node.nodeId)
                    is SortedEntryInfo -> _requestOpenEntry.emit(node)
                }
            }
        }
    }

    fun onEditNodeClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            finishNodeAction()
            viewModelScope.launch {
                when (val node = nodes[0]) {
                    is GroupInfo -> _requestEditGroup.emit(node)
                    is EntryInfo -> _requestEditEntry.emit(node)
                }
            }
        }
    }

    fun onCopyNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    selectionMode = false,
                    pasteMode = PasteMode.PASTE_FROM_COPY
                )
            }
            viewModelScope.launch {
                _requestCopyNodes.emit(nodes.toNodes())
            }
        }
    }

    fun onMoveNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    selectionMode = false,
                    pasteMode = PasteMode.PASTE_FROM_MOVE
                )
            }
            viewModelScope.launch {
                _requestMoveNodes.emit(nodes.toNodes())
            }
        }
    }

    fun onDeleteNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            viewModelScope.launch {
                _requestDeleteNodes.emit(DeleteActionState(
                    nodes = nodes.toNodes(),
                    isRecycleBin = false
                ))
            }
            finishNodeAction()
        }
    }

    fun onPasteNodesClicked() {
        val state = _nodeActionState.value
        mainGroup?.nodeId?.let { parentId ->
            viewModelScope.launch {
                _requestPaste.emit(
                    PasteActionState(
                        state.pasteMode,
                        parentId,
                        state.selectedNodes.toNodes()
                    )
                )
            }
            finishNodeAction()
        }
    }

    fun onActionCreated() {
        _groupUIState.update { groupState ->
            groupState.copy(showAddNodeButton = false)
        }
    }

    fun onActionDestroyed() {
        viewModelScope.launch {
            _removeNodeAction.emit(Unit)
        }
        clearNodeAction()
        _groupUIState.update { groupState ->
            groupState.copy(showAddNodeButton = mAddNodeButtonAllowed)
        }
    }

    fun actionNodeInProgress(): Boolean {
        return _nodeActionState.value.isActive
    }

    fun finishNodeAction() {
        _nodeActionState.update { it.copy(isActive = false) }
    }

    fun clearNodeAction() {
        _nodeActionState.value = NodeActionState()
    }

    fun recycleBinActionsAllowed(): Boolean = mRecycleBinAllowed && mRecyclingBinIsCurrentGroup

    fun emptyRecycleBin() {
        viewModelScope.launch {
            if (recycleBinActionsAllowed()) {
                withContext(Dispatchers.IO) {
                    mDatabase?.getNodesInRecycleBin()?.let { nodesToDelete ->
                        // Automatically delete all elements
                        if (!nodesToDelete.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                _requestDeleteNodes.emit(
                                    DeleteActionState(
                                        nodes = nodesToDelete,
                                        isRecycleBin = true
                                    )
                                )
                            }
                        }
                    }
                }
                finishNodeAction()
            }
        }
    }

    fun containsRecycleBin(
        database: ContextualDatabase?,
        nodes: List<NodeInfo>
    ): Boolean {
        return nodes.any { it is GroupInfo && it.isRecycleBin(database) }
    }

    fun previousGroupExists(): Boolean = mPreviousGroupsStates.isNotEmpty()

    fun loadPreviousGroup() {
        if (previousGroupExists()) {
            return loadMainGroup(
                database = mDatabase,
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

    data class NodeActionState(
        val isActive: Boolean = false,
        val selectionMode: Boolean = false,
        val pasteMode: PasteMode = PasteMode.UNDEFINED,
        val selectedNodes: List<SortedNodeInfo> = emptyList()
    )

    data class DeleteActionState(
        val nodes: Nodes,
        val isRecycleBin: Boolean
    )

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
        var groupId: GroupId?,
        var firstVisibleItem: Int?
    )

    data class PasteActionState(
        val pasteMode: PasteMode,
        val parentId: GroupId,
        val nodes: Nodes
    )

    data class SortNode(
        val sortNodeEnum: SortNodeEnum? = null,
        val sortNodeParameters: SortNodeEnum.SortNodeParameters? = null,
        val sortDatabaseParameters: SortNodeEnum.SortDatabaseParameters? = null
    )

    enum class PasteMode {
        UNDEFINED, PASTE_FROM_COPY, PASTE_FROM_MOVE
    }

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}