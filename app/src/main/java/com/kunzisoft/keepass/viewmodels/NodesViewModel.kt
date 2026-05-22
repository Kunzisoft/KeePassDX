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
import com.kunzisoft.keepass.model.NodeInfo

class NodesViewModel: ViewModel() {

    val nodesToDelete : LiveData<List<NodeInfo>> get() = _nodesToDelete
    private val _nodesToDelete = MutableLiveData<List<NodeInfo>>()

    val nodesToPermanentlyDelete : LiveData<List<NodeInfo>> get() = _nodesToPermanentlyDelete
    private val _nodesToPermanentlyDelete = SingleLiveEvent<List<NodeInfo>>()

    fun deleteNodes(nodes: List<NodeInfo>) {
        this._nodesToDelete.value = nodes.toList()
    }

    fun permanentlyDeleteNodes(nodes: List<NodeInfo>) {
        this._nodesToDelete.value = listOf()
        this._nodesToPermanentlyDelete.value = nodes
    }
}