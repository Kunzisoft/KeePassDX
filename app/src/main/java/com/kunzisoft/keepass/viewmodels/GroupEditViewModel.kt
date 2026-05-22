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
 *
 */
package com.kunzisoft.keepass.viewmodels

import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.GroupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GroupEditViewModel: NodeEditViewModel() {

    private val mGroupEditState = MutableStateFlow<GroupEditState>(GroupEditState.Wait)
    val groupEditState: StateFlow<GroupEditState> = mGroupEditState.asStateFlow()

    var groupNamesNotAllowed : List<String>? = null

    fun approveGroupCreation(groupInfo: GroupInfo) {
        this.mGroupEditState.value = GroupEditState.CreateGroup(groupInfo)
    }

    fun approveGroupUpdate(groupInfo: GroupInfo) {
        // Assume it's only possible with UUID in KDBX database
        this.mGroupEditState.value = GroupEditState.UpdateGroup(groupInfo.nodeId, groupInfo)
    }

    fun actionPerformed() {
        this.mGroupEditState.value = GroupEditState.Wait
    }

    sealed class GroupEditState {
        object Wait: GroupEditState()
        data class CreateGroup(
            val groupInfo: GroupInfo
        ): GroupEditState()
        data class UpdateGroup(
            var oldGroupId: NodeId<*>,
            val groupInfo: GroupInfo
        ): GroupEditState()
    }
}