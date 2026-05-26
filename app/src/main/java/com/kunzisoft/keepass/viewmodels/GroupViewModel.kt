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
import com.kunzisoft.keepass.activities.GroupActivity.GroupState
import com.kunzisoft.keepass.activities.GroupActivity.SearchState
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SearchGroupInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * ViewModel for managing groups.
 */
class GroupViewModel(application: Application): AndroidViewModel(application) {

    // Main group currently in memory
    var mMainGroup: GroupInfo? = null
        private set

    // Group state, not a search
    var mMainGroupState: GroupState? = null

    // Group currently visible (search or main group)
    var mCurrentGroup: GroupInfo? = null


    private var mPreviousGroupsIds = mutableListOf<GroupState>()

    var mSearchState: SearchState? = null
    // To mainly manage keyboard
    var mAutoSearch: Boolean = false
    // To manage temp search
    var mTempSearchInfo: Boolean = false
    var mLockSearchListeners = false

    var mRequestStartupSearch = true

    var recyclingBinIsCurrentGroup = false
        private set

    private val _groupUIState = MutableStateFlow<GroupUISTate?>(null)
    val groupUIState: StateFlow<GroupUISTate?> = _groupUIState.asStateFlow()

    private val _onGroupLoaded = MutableSharedFlow<GroupInfo>(replay = 0)
    val onGroupLoaded: SharedFlow<GroupInfo> = _onGroupLoaded.asSharedFlow()

    private var mDefaultSearchParameters: SearchParameters = PreferencesUtil.getDefaultSearchParameters(getApplication())

    fun loadMainGroup(
        database: ContextualDatabase?,
        groupId: NodeId<*>?,
        showFromPosition: Int?
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val group = if (groupId != null) {
                    database?.getGroupInfoById(groupId)
                } else {
                    database?.getRootGroupInfo()
                }
                if (group != null) {
                    recyclingBinIsCurrentGroup = group.isRecycleBin(database)
                    mMainGroup = group
                    // Save group state
                    mMainGroupState = GroupState(group.nodeId, showFromPosition)
                    mCurrentGroup = group
                    mSearchState = null
                    _onGroupLoaded.emit(group)
                    _groupUIState.value = GroupUISTate(
                        group = group,
                        showFromPosition = showFromPosition
                    )
                }
            }
        }
    }

    fun loadChildGroup(
        database: ContextualDatabase?,
        groupId: NodeId<*>?
    ) {
        // Save the last not virtual group and it's position
        if (mCurrentGroup !is SearchGroupInfo) {
            mMainGroupState?.let {
                mPreviousGroupsIds.add(it)
            }
        }
        loadMainGroup(database, groupId, showFromPosition = 0)
    }

    fun loadSearch(
        database: ContextualDatabase?,
        searchState: SearchState,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val searchParameters = searchState.searchParameters
                val showFromPosition = searchState.firstVisibleItem
                val group = database?.createSearchGroupInfo(
                    searchParameters = searchParameters,
                    fromGroup = mMainGroupState?.groupId,
                    max = SearchHelper.MAX_SEARCH_ENTRY
                )
                if (group != null) {
                    mCurrentGroup = group
                    mSearchState = searchState
                    _onGroupLoaded.emit(group)
                    _groupUIState.value = GroupUISTate(
                        group = group,
                        showFromPosition = showFromPosition
                    )
                }
            }
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

    fun isCurrentGroupRoot(database: ContextualDatabase?): Boolean {
        return mCurrentGroup?.isRoot(database) == true
    }

    fun previousGroupExists(): Boolean = mPreviousGroupsIds.isNotEmpty()

    fun loadPreviousGroup(database: ContextualDatabase?) {
        if (previousGroupExists()) {
            val previousGroup = mPreviousGroupsIds.removeAt(mPreviousGroupsIds.lastIndex)
            return loadMainGroup(
                database = database,
                groupId = previousGroup.groupId,
                showFromPosition = previousGroup.firstVisibleItem
            )
        }
    }

    fun clearSearch() {
        mSearchState = null
        mTempSearchInfo = false
    }

    data class GroupUISTate(
        val loaded: Boolean = false,
        val group: GroupInfo,
        var showFromPosition: Int?
    )

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}