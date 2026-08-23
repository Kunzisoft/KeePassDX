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

import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.model.GroupInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GroupEditViewModel: NodeEditViewModel() {

    private val _onCreateGroup = MutableSharedFlow<GroupInfo>(replay = 0)
    val onCreateGroup: SharedFlow<GroupInfo> = _onCreateGroup.asSharedFlow()

    private val _onUpdateGroup = MutableSharedFlow<GroupInfo>(replay = 0)
    val onUpdateGroup: SharedFlow<GroupInfo> = _onUpdateGroup.asSharedFlow()

    var groupNamesNotAllowed : List<String>? = null

    fun approveGroupCreation(groupInfo: GroupInfo) {
        viewModelScope.launch {
            _onCreateGroup.emit(groupInfo)
        }
    }

    fun approveGroupUpdate(groupInfo: GroupInfo) {
        viewModelScope.launch {
            _onUpdateGroup.emit(groupInfo)
        }
    }
}