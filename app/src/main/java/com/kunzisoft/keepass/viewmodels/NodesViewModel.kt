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
import com.kunzisoft.keepass.database.element.node.Nodes

class NodesViewModel: ViewModel() {

    // TODO Flows
    val nodesToDelete : LiveData<Nodes> get() = _nodesToDelete
    private val _nodesToDelete = MutableLiveData<Nodes>()

    val nodesToPermanentlyDelete : LiveData<Nodes> get() = _nodesToPermanentlyDelete
    private val _nodesToPermanentlyDelete = SingleLiveEvent<Nodes>()

    fun deleteNodes(nodes: Nodes) {
        this._nodesToDelete.value = nodes
    }

    fun permanentlyDeleteNodes(nodes: Nodes) {
        this._nodesToDelete.value = Nodes()
        this._nodesToPermanentlyDelete.value = nodes
    }
}