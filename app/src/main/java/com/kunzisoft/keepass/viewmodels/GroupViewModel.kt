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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.utils.IOActionTask
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * ViewModel for managing groups.
 */
class GroupViewModel: ViewModel() {

    // TODO As UIState
    private val _mainGroup = MutableSharedFlow<SuperGroup>(replay = 1)
    val mainGroup: SharedFlow<SuperGroup> = _mainGroup.asSharedFlow()

    private val _group = MutableStateFlow<SuperGroup?>(null)
    val group: StateFlow<SuperGroup?> = _group.asStateFlow()

    private val _firstPositionVisible = MutableStateFlow(0)
    val firstPositionVisible: StateFlow<Int> = _firstPositionVisible.asStateFlow()

    fun loadMainGroup(
        database: ContextualDatabase?,
        groupId: NodeId<*>?,
        showFromPosition: Int?
    ) {
        IOActionTask(
            scope = viewModelScope,
            action = {
                if (groupId != null) {
                    database?.getGroupInfoById(groupId)
                } else {
                    database?.getRootGroupInfo()
                }
            },
            onActionComplete = { group ->
                if (group != null) {
                    val superGroup = SuperGroup(
                        group = group,
                        isRecycleBin = group.isRecycleBin(database),
                        showFromPosition = showFromPosition
                    )
                    viewModelScope.launch {
                        _mainGroup.emit(superGroup)
                    }
                    _group.value = superGroup
                }
            }
        ).execute()
    }

    fun loadSearchGroup(
        database: ContextualDatabase?,
        searchParameters: SearchParameters,
        fromGroup: NodeId<*>?,
        showFromPosition: Int?
    ) {
        IOActionTask(
            scope = viewModelScope,
            action = {
                database?.createSearchGroupInfo(
                    searchParameters,
                    fromGroup,
                    SearchHelper.MAX_SEARCH_ENTRY
                )
            },
            onActionComplete = { group ->
                if (group != null) {
                    _group.value = SuperGroup(
                        group = group,
                        isRecycleBin = group.isRecycleBin(database),
                        showFromPosition = showFromPosition,
                        searchParameters = searchParameters
                    )
                }
            }
        ).execute()
    }

    fun assignPosition(position: Int) {
        _firstPositionVisible.value = position
    }

    data class SuperGroup(
        val group: GroupInfo,
        val isRecycleBin: Boolean,
        var showFromPosition: Int?,
        var searchParameters: SearchParameters = SearchParameters()
    )

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}