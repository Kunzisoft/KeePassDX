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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.utils.IOActionTask
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters


class GroupViewModel: ViewModel() {

    val mainGroup : LiveData<SuperGroup> get() = _mainGroup
    private val _mainGroup = MutableLiveData<SuperGroup>()

    val group : LiveData<SuperGroup> get() = _group
    private val _group = MutableLiveData<SuperGroup>()

    val firstPositionVisible : LiveData<Int> get() = _firstPositionVisible
    private val _firstPositionVisible = MutableLiveData<Int>()

    fun loadMainGroup(database: ContextualDatabase?,
                      groupId: NodeId<*>?,
                      showFromPosition: Int?) {
        IOActionTask(
            {
                if (groupId != null) {
                    database?.getGroupById(groupId)
                } else {
                    database?.rootGroup
                }
            },
            { group ->
                if (group != null) {
                    _mainGroup.value = SuperGroup(group,
                        database?.recycleBin == group,
                        showFromPosition)
                    _group.value = _mainGroup.value
                }
            }
        ).execute()
    }

    fun loadSearchGroup(database: ContextualDatabase?,
                        searchParameters: SearchParameters,
                        fromGroup: NodeId<*>?,
                        showFromPosition: Int?) {
        IOActionTask(
            {
                database?.createVirtualGroupFromSearch(
                    searchParameters,
                    fromGroup,
                    SearchHelper.MAX_SEARCH_ENTRY
                )
            },
            { group ->
                if (group != null) {
                    _group.value = SuperGroup(group,
                        database?.recycleBin == group,
                        showFromPosition,
                        searchParameters)
                }
            }
        ).execute()
    }

    fun assignPosition(position: Int) {
        _firstPositionVisible.value = position
    }

    data class SuperGroup(val group: Group,
                          val isRecycleBin: Boolean,
                          var showFromPosition: Int?,
                          var searchParameters: SearchParameters = SearchParameters())

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}