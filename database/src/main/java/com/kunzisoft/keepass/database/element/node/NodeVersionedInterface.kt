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

import android.os.Parcelable
import com.kunzisoft.keepass.database.element.icon.IconImage

/**
 * Interface representing a versioned node in the database hierarchy.
 * @param ParentGroup The type of the parent group.
 */
interface NodeVersionedInterface<ParentGroup> : NodeTimeInterface,  Parcelable {

    /**
     * The title of the node.
     */
    var title: String

    /**
     * The icon associated with the node.
     */
    var icon: IconImage

    /**
     * The type of the node.
     */
    val type: NodeType

    /**
     * The parent node of this versioned node.
     */
    var parent: ParentGroup?

    /**
     * Checks if the node has a parent.
     * @return True if a parent exists, false otherwise.
     */
    fun containsParent(): Boolean

    /**
     * Callback method called after a new parent has been assigned to the node.
     */
    fun afterAssignNewParent()

    /**
     * Checks if this node is contained within the specified container group.
     * @param container The group to check against.
     * @return True if contained in the group, false otherwise.
     */
    fun isContainedIn(container: ParentGroup): Boolean

    /**
     * Retrieves the index of this node within its parent group.
     * Groups are always before in natural order (DB order).
     * @return The index in the parent.
     */
    fun indexInParent(): Int

    /**
     * Updates the access or modification time of the node.
     * @param modified Whether the node has been modified.
     * @param touchParents Whether to also touch the parent nodes.
     */
    fun touch(
        modified: Boolean,
        touchParents: Boolean,
    )
}