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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.GroupId
import com.kunzisoft.keepass.database.element.SortNodeEnum
import com.kunzisoft.keepass.database.element.node.DefaultNodeFilter
import com.kunzisoft.keepass.database.element.node.EmptyNodeFilter
import com.kunzisoft.keepass.database.element.node.NodeFilter
import com.kunzisoft.keepass.database.element.node.Nodes
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.NodeInfo
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

    private var mRecyclingBinIsCurrentGroup = false

    private var mAddGroupOrEntryAllowed = false
    private val mAddNodeButtonAllowed: Boolean
        get() = mAddGroupOrEntryAllowed && !actionNodeInProgress()

    // Main group currently in memory
    var mainGroup: GroupInfo? = null
        private set

    // Group state to retrieve position, not a search
    private var mMainGroupState: GroupState? = null
    // Previous groups states to retrieve history and position
    private var mPreviousGroupsStates = mutableListOf<GroupState>()

    // Group currently visible (search or main group)
    val isCurrentGroupIsRoot: Boolean
        get() = groupUIState.value.group?.isRoot(mDatabase) == true

    /*
     * Manage action in group
     */
    private val _nodeActionState = MutableStateFlow(NodeActionState())
    val nodeActionState: StateFlow<NodeActionState> = _nodeActionState.asStateFlow()

    // Derived StateFlow from nodeActionState.selectedNodes to keep sync
    val actionsNodes: StateFlow<List<SortedNodeInfo>> = nodeActionState
        .map { it.selectedNodes }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    private val _groupUIState = MutableStateFlow<GroupUISTate>(GroupUISTate())
    val groupUIState: StateFlow<GroupUISTate> = _groupUIState.asStateFlow()

    private val _viewEvent = MutableSharedFlow<GroupEvent>(replay = 0)
    val viewEvent: SharedFlow<GroupEvent> = _viewEvent.asSharedFlow()

    private var mAutoFocusSearch: Boolean = false
    private var mRecursiveNumberEntries: Boolean = false
    private var mShowExpiredEntries: Boolean = false
    private var mShowTemplates: Boolean = false
    private var mNodeFilter: NodeFilter = EmptyNodeFilter()

    fun assignPreferences() {
        mAutoFocusSearch = PreferencesUtil.automaticallyFocusSearch(getApplication())
        mRecursiveNumberEntries = PreferencesUtil.recursiveNumberEntries(getApplication())
        mShowExpiredEntries = PreferencesUtil.showExpiredEntries(getApplication())
        mShowTemplates = PreferencesUtil.showTemplates(getApplication())
    }

    fun onDatabaseLoaded(database: ContextualDatabase) {
        this.mDatabase = database
        this.mNodeFilter = DefaultNodeFilter(
            database = mDatabase,
            showExpired = mShowExpiredEntries,
            showTemplates = mShowTemplates
        )
    }

    fun loadGroup() {
        loadMainGroup()
    }

    private fun loadMainGroup(
        groupState: GroupState? = mMainGroupState
    ) {
        viewModelScope.launch {
            mDatabase?.let { database ->
                _groupUIState.update { groupState ->
                    groupState.copy(loaded = false)
                }
                withContext(Dispatchers.IO) {
                    val groupId = groupState?.groupId
                    val showFromPosition = groupState?.firstVisibleItem
                    val showFromOffset = groupState?.firstVisibleItemOffset ?: 0
                    val group = if (groupId != null) {
                        database.getGroupInfoById(groupId)
                    } else {
                        database.getRootGroupInfo()
                    }
                    if (group != null) {
                        mRecyclingBinIsCurrentGroup = group.isRecycleBin(database)
                        mainGroup = group
                        // Save group state
                        mMainGroupState = GroupState(group.nodeId, showFromPosition, showFromOffset)
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
                        val addGroupEnabled = database.allowAddGroupIn(group = group)
                        val addEntryEnabled = database.allowAddEntryIn(group = group)
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
                                _viewEvent.emit(GroupEvent.ShowPosition(showFromPosition, showFromOffset))
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            loadPreviousGroup()
                        }
                    }
                }
            }
        }
    }

    fun loadGroup(
        groupId: GroupId?
    ) {
        // Save the last not virtual group and it's position
        mMainGroupState?.let {
            mPreviousGroupsStates.add(it)
        }
        loadMainGroup(GroupState(groupId, firstVisibleItem = 0))
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
            _viewEvent.emit(GroupEvent.SortSelected(SortNode(
                sortNodeEnum = sortNode,
                sortNodeParameters = sortNodeParameters,
                sortDatabaseParameters = sortDatabaseParameters
            )))
        }
    }

    fun assignPosition(
        position: Int = 0,
        offset: Int = 0
    ) {
        mMainGroupState?.let {
            it.firstVisibleItem = position
            it.firstVisibleItemOffset = offset
        }
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
                _viewEvent.emit(GroupEvent.ShowGroup(currentGroup))
            } else {
                when (nodeActionState.value.nodeActionMode) {
                    NodeActionMode.SELECTION -> finishNodeAction()
                    else -> {}
                }
                _viewEvent.emit(GroupEvent.OpenGroup(group.nodeId))
            }
        }
    }

    fun onBreadcrumbLongClicked(group: SortedGroupInfo) {
        viewModelScope.launch {
            val currentGroup = mainGroup
            if (mDatabase?.isReadOnly == false
                && currentGroup != null
                && group.nodeId == currentGroup.nodeId
                && (mDatabase?.rootGroupIsVirtual != true
                        || currentGroup.nodeId != mDatabase?.rootGroup?.nodeId)
            ) {
                finishNodeAction()
                _viewEvent.emit(GroupEvent.EditGroup(currentGroup))
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
            val state = nodeActionState.value
            when (state.nodeActionMode) {
                NodeActionMode.SELECTION -> {
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
                }
                NodeActionMode.PASTE_FROM_COPY,
                NodeActionMode.PASTE_FROM_MOVE -> {
                    when (node) {
                        is SortedGroupInfo -> _viewEvent.emit(GroupEvent.OpenGroup(node.nodeId))
                        else -> {}
                    }
                }
                null -> {
                    when (node) {
                        is SortedGroupInfo -> _viewEvent.emit(GroupEvent.OpenGroup(node.nodeId))
                        is SortedEntryInfo -> _viewEvent.emit(GroupEvent.OpenEntry(node))
                    }
                }
            }
        }
    }

    fun performKeeShareClick(node: SortedGroupInfo) {
        viewModelScope.launch {
            _viewEvent.emit(GroupEvent.KeeShareClicked(node.nodeId))
        }
    }

    fun performLongNodeClick(node: SortedNodeInfo): Boolean {
        viewModelScope.launch {
            val state = nodeActionState.value
            when (state.nodeActionMode) {
                NodeActionMode.SELECTION,
                NodeActionMode.PASTE_FROM_COPY,
                NodeActionMode.PASTE_FROM_MOVE -> {
                    performNodeClick(node)
                }
                null -> {
                    // Select the first item after a long click
                    val selectedNodes = state.selectedNodes.toMutableList()
                    if (selectedNodes.none { it.nodeId == node.nodeId })
                        selectedNodes.add(node)
                    selectNodes(selectedNodes)
                    _viewEvent.emit(GroupEvent.HideKeyboard)
                }
            }
        }
        return true
    }

    fun selectNodes(nodes: List<SortedNodeInfo>): Boolean {
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    isActive = true,
                    nodeActionMode = NodeActionMode.SELECTION,
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
                    is SortedGroupInfo -> _viewEvent.emit(GroupEvent.OpenGroup(node.nodeId))
                    is SortedEntryInfo -> _viewEvent.emit(GroupEvent.OpenEntry(node))
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
                    is GroupInfo -> _viewEvent.emit(GroupEvent.EditGroup(node))
                    is EntryInfo -> _viewEvent.emit(GroupEvent.EditEntry(node))
                }
            }
        }
    }

    fun onCopyNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    nodeActionMode = NodeActionMode.PASTE_FROM_COPY
                )
            }
            viewModelScope.launch {
                _viewEvent.emit(GroupEvent.CopyNodes(nodes.toNodes()))
            }
        }
    }

    fun onMoveNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            _nodeActionState.update {
                it.copy(
                    nodeActionMode = NodeActionMode.PASTE_FROM_MOVE
                )
            }
            viewModelScope.launch {
                _viewEvent.emit(GroupEvent.MoveNodes(nodes.toNodes()))
            }
        }
    }

    fun onDeleteNodesClicked() {
        val nodes = _nodeActionState.value.selectedNodes
        if (nodes.isNotEmpty()) {
            viewModelScope.launch {
                _viewEvent.emit(GroupEvent.DeleteNodes(DeleteActionState(
                    nodes = nodes.toNodes(),
                    isRecycleBin = false
                )))
            }
            finishNodeAction()
        }
    }

    fun onPasteNodesClicked() {
        val state = _nodeActionState.value
        mainGroup?.nodeId?.let { parentId ->
            viewModelScope.launch {
                state.nodeActionMode?.let { pasteMode ->
                    _viewEvent.emit(
                        GroupEvent.PasteNodes(
                            PasteActionState(
                                pasteMode,
                                parentId,
                                state.selectedNodes.toNodes()
                            )
                        )
                    )
                }
                _nodeActionState.update {
                    it.copy(
                        nodeActionMode = null
                    )
                }
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
            _viewEvent.emit(GroupEvent.RemoveNodeAction)
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

    fun databaseActionsAllowed(): Boolean = groupUIState.value.loaded
    fun saveDatabaseActionAllowed(): Boolean = mDatabase?.isReadOnly == false
    fun mergeDatabaseActionAllowed(): Boolean = saveDatabaseActionAllowed() && mDatabase?.isMergeDataAllowed() == true
    fun reloadDatabaseActionAllowed(): Boolean = true
    fun mergeFromDatabaseActionAllowed(): Boolean = mergeDatabaseActionAllowed()
    fun copyToDatabaseActionAllowed(): Boolean = true

    fun recycleBinActionsAllowed(): Boolean = saveDatabaseActionAllowed()
            && mDatabase?.isRecycleBinEnabled == true && mRecyclingBinIsCurrentGroup

    fun emptyRecycleBin() {
        viewModelScope.launch {
            if (recycleBinActionsAllowed()) {
                val nodesToDelete = withContext(Dispatchers.IO) {
                    mDatabase?.getNodesInRecycleBin()
                }
                // Automatically delete all elements
                if (nodesToDelete != null && !nodesToDelete.isEmpty()) {
                    _viewEvent.emit(
                        GroupEvent.DeleteNodes(
                            DeleteActionState(
                                nodes = nodesToDelete,
                                isRecycleBin = true,
                            )
                        )
                    )
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
                groupState = mPreviousGroupsStates.removeAt(mPreviousGroupsStates.lastIndex)
            )
        }
    }

    fun scrollTo(dy: Int) {
        viewModelScope.launch {
            if (!actionNodeInProgress())
                _viewEvent.emit(GroupEvent.ScrollTo(dy))
        }
    }

    fun hideKeyboard() {
        viewModelScope.launch {
            _viewEvent.emit(GroupEvent.HideKeyboard)
        }
    }

    data class NodeActionState(
        val isActive: Boolean = false,
        val nodeActionMode: NodeActionMode? = null,
        val selectedNodes: List<SortedNodeInfo> = emptyList()
    )

    sealed class GroupEvent {
        object RemoveNodeAction : GroupEvent()
        data class OpenGroup(val groupId: GroupId) : GroupEvent()
        data class OpenEntry(val entry: EntryInfo) : GroupEvent()
        data class EditGroup(val group: GroupInfo) : GroupEvent()
        data class EditEntry(val entry: EntryInfo) : GroupEvent()
        data class CopyNodes(val nodes: Nodes) : GroupEvent()
        data class MoveNodes(val nodes: Nodes) : GroupEvent()
        data class DeleteNodes(val deleteActionState: DeleteActionState) : GroupEvent()
        data class PasteNodes(val pasteActionState: PasteActionState) : GroupEvent()
        data class ShowGroup(val group: GroupInfo) : GroupEvent()
        object HideKeyboard : GroupEvent()
        data class ShowPosition(val position: Int, val offset: Int) : GroupEvent()
        data class ScrollTo(val dy: Int) : GroupEvent()
        data class SortSelected(val sortNode: SortNode) : GroupEvent()
        object ClearSearch : GroupEvent()
        data class KeeShareClicked(val groupId: GroupId) : GroupEvent()
    }

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
        var firstVisibleItem: Int?,
        var firstVisibleItemOffset: Int = 0
    )

    data class PasteActionState(
        val pasteMode: NodeActionMode,
        val parentId: GroupId,
        val nodes: Nodes
    )

    data class SortNode(
        val sortNodeEnum: SortNodeEnum? = null,
        val sortNodeParameters: SortNodeEnum.SortNodeParameters? = null,
        val sortDatabaseParameters: SortNodeEnum.SortDatabaseParameters? = null
    )

    enum class NodeActionMode {
        SELECTION, PASTE_FROM_COPY, PASTE_FROM_MOVE
    }

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}