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
package com.kunzisoft.keepass.database.element.node

import com.kunzisoft.keepass.database.element.Group

interface Node: NodeVersionedInterface<Group> {

    val nodeId: NodeId<*>?

    val nodePositionInParent: Int
        get() {
            parent?.getChildren(Group.ChildFilter.META_STREAM)?.let { children ->
                children.forEachIndexed { index, nodeVersioned ->
                    if (nodeVersioned.nodeId == this.nodeId)
                        return index
                }
            }
            return -1
        }

    fun addParentFrom(node: Node) {
        parent = node.parent
    }

    fun removeParent() {
        parent = null
    }
}

/**
 * Type of available Nodes
 */
enum class Type {
    GROUP, ENTRY
}


