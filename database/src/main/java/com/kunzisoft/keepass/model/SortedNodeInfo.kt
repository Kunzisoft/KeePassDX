/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeNaturalOrderInterface
import com.kunzisoft.keepass.database.element.node.NodeTimeInterface

/**
 * Interface for node information that includes sorting and hierarchy data.
 * Combines time tracking and natural ordering capabilities.
 */
interface SortedNodeInfo : NodeTimeInterface, NodeNaturalOrderInterface {
    /**
     * Unique identifier for the node.
     */
    val nodeId: NodeId<*>

    /**
     * The title of the node.
     */
    val title: String

    /**
     * The icon associated with the node.
     */
    val icon: IconImage

    /**
     * Tags associated with the node.
     */
    val tags: Tags

    /**
     * The path of the node in the database.
     */
    val path: String?
}