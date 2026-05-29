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
import com.kunzisoft.keepass.database.element.node.Nodes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * View model for nodes.
 */
class NodesViewModel : ViewModel() {

    private val _nodesToDelete = MutableStateFlow(Nodes())

    /**
     * Nodes to delete.
     */
    val nodesToDelete: StateFlow<Nodes> = _nodesToDelete.asStateFlow()

    private val _nodesToPermanentlyDelete = MutableSharedFlow<Nodes>()

    /**
     * Nodes to permanently delete.
     */
    val nodesToPermanentlyDelete: SharedFlow<Nodes> = _nodesToPermanentlyDelete.asSharedFlow()

    /**
     * Delete nodes.
     * @param nodes Nodes to delete.
     */
    fun deleteNodes(nodes: Nodes) {
        _nodesToDelete.value = nodes
    }

    /**
     * Permanently delete nodes.
     * @param nodes Nodes to permanently delete.
     */
    fun permanentlyDeleteNodes(nodes: Nodes) {
        _nodesToDelete.value = Nodes()
        viewModelScope.launch {
            _nodesToPermanentlyDelete.emit(nodes)
        }
    }
}
