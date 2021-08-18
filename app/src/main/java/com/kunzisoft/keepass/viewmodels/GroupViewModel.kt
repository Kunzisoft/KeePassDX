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
import com.kunzisoft.keepass.activities.GroupActivity
import com.kunzisoft.keepass.app.database.IOActionTask
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.node.Node


class GroupViewModel: ViewModel() {

    val group : LiveData<SuperGroup> get() = _group
    private val _group = MutableLiveData<SuperGroup>()

    val firstPositionVisible : LiveData<Int> get() = _firstPositionVisible
    private val _firstPositionVisible = MutableLiveData<Int>()

    fun loadGroup(database: Database?,
                  groupState: GroupActivity.GroupState?) {
        IOActionTask(
            {
                val groupId = groupState?.groupId
                if (groupId != null) {
                    database?.getGroupById(groupId)
                } else {
                    database?.rootGroup
                }
            },
            { group ->
                if (group != null) {
                    _group.value = SuperGroup(group,
                        database?.recycleBin == group,
                        groupState?.firstVisibleItem)
                }
            }
        ).execute()
    }

    fun loadGroup(database: Database?,
                  group: Group,
                  showFromPosition: Int?) {
        _group.value = SuperGroup(group,
            database?.recycleBin == group,
            showFromPosition)
    }

    fun assignPosition(position: Int) {
        _firstPositionVisible.value = position
    }

    fun loadGroupFromSearch(database: Database?,
                            searchQuery: String,
                            omitBackup: Boolean) {
        IOActionTask(
            {
                database?.createVirtualGroupFromSearch(searchQuery, omitBackup)
            },
            { group ->
                if (group != null) {
                    _group.value = SuperGroup(group,
                        database?.recycleBin == group,
                        0)
                }
            }
        ).execute()
    }

    data class SuperGroup(val group: Group, val isRecycleBin: Boolean, var showFromPosition: Int?)

    companion object {
        private val TAG = GroupViewModel::class.java.name
    }
}