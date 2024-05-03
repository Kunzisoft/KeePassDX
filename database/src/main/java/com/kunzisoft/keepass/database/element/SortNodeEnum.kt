/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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

package com.kunzisoft.keepass.database.element

import com.kunzisoft.keepass.database.element.group.GroupVersionedInterface
import com.kunzisoft.keepass.database.element.node.NodeVersionedInterface
import com.kunzisoft.keepass.database.element.node.Type
import java.util.*

enum class SortNodeEnum {
    DB, TITLE, USERNAME, CREATION_TIME, LAST_MODIFY_TIME, LAST_ACCESS_TIME;

    fun <G: GroupVersionedInterface<G, *>> getNodeComparator(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : Comparator<NodeVersionedInterface<G>> {
        return when (this) {
            DB -> NodeNaturalComparator(database, sortNodeParameters) // Force false because natural order contains recycle bin
            TITLE -> NodeTitleComparator(database, sortNodeParameters)
            USERNAME -> NodeUsernameComparator(database, sortNodeParameters)
            CREATION_TIME -> NodeCreationComparator(database, sortNodeParameters)
            LAST_MODIFY_TIME -> NodeLastModificationComparator(database, sortNodeParameters)
            LAST_ACCESS_TIME -> NodeLastAccessComparator(database, sortNodeParameters)
        }
    }

    data class SortNodeParameters(var ascending: Boolean = true,
                                  var groupsBefore: Boolean = true,
                                  var recycleBinBottom: Boolean = true)

    abstract class NodeComparator
            <
                G: GroupVersionedInterface<*, *>,
                T: NodeVersionedInterface<G>
            >(var database: Database, var sortNodeParameters: SortNodeParameters)
        : Comparator<T> {

        abstract fun compareBySpecificOrder(object1: T, object2: T): Int

        private fun specificOrderOrHashIfEquals(object1: T, object2: T): Int {
            val specificOrderComp = compareBySpecificOrder(object1, object2)
            return when {
                specificOrderComp == 0 -> object1.hashCode() - object2.hashCode()
                sortNodeParameters.ascending -> specificOrderComp
                else -> -specificOrderComp
            }
        }

        override fun compare(object1: T, object2: T): Int {
            if (object1 == object2)
                return 0

            when (object1.type) {
                Type.GROUP -> {
                    when (object2.type) {
                        Type.GROUP -> {
                            // RecycleBin at end of groups
                            if (database.isRecycleBinEnabled && sortNodeParameters.recycleBinBottom) {
                                if (database.recycleBin == object1)
                                    return 1
                                if (database.recycleBin == object2)
                                    return -1
                            }
                            return specificOrderOrHashIfEquals(object1, object2)
                        }
                        Type.ENTRY -> {
                            return if (sortNodeParameters.groupsBefore)
                                -1
                            else
                                1
                        }
                    }
                }
                Type.ENTRY -> {
                    return when (object2.type) {
                        Type.GROUP -> {
                            if (sortNodeParameters.groupsBefore)
                                1
                            else
                                -1
                        }
                        Type.ENTRY -> {
                            specificOrderOrHashIfEquals(object1, object2)
                        }
                    }
                }
            }
        }
    }

    /**
     * Comparator of node by natural database placement
     */
    class NodeNaturalComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            return object1.nodeIndexInParentForNaturalOrder()
                    .compareTo(object2.nodeIndexInParentForNaturalOrder())
        }
    }

    /**
     * Comparator of Node by Title
     */
    class NodeTitleComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val titleCompare = object1.title.compareTo(object2.title, ignoreCase = true)
            return if (titleCompare == 0)
                NodeNaturalComparator<G, T>(database, sortNodeParameters)
                        .compare(object1, object2)
            else
                titleCompare
        }
    }

    /**
     * Comparator of Node by Username, Groups by title
     */
    class NodeUsernameComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            return if (object1.type == Type.ENTRY && object2.type == Type.ENTRY) {
                // To get username if it's a ref
                val usernameCompare = (object1 as Entry).getEntryInfo(database).username
                        .compareTo((object2 as Entry).getEntryInfo(database).username,
                                ignoreCase = true)
                if (usernameCompare == 0)
                    NodeTitleComparator<G, T>(database, sortNodeParameters)
                            .compare(object1, object2)
                else
                    usernameCompare
            } else {
                NodeTitleComparator<G, T>(database, sortNodeParameters)
                        .compare(object1, object2)
            }
        }
    }

    /**
     * Comparator of node by creation
     */
    class NodeCreationComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val creationCompare = object1.creationTime.date
                    .compareTo(object2.creationTime.date)
            return if (creationCompare == 0)
                NodeNaturalComparator<G, T>(database, sortNodeParameters)
                        .compare(object1, object2)
            else
                creationCompare
        }
    }

    /**
     * Comparator of node by last modification
     */
    class NodeLastModificationComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val lastModificationCompare = object1.lastModificationTime.date
                    .compareTo(object2.lastModificationTime.date)
            return if (lastModificationCompare == 0)
                NodeNaturalComparator<G, T>(database, sortNodeParameters)
                        .compare(object1, object2)
            else
                lastModificationCompare
        }
    }

    /**
     * Comparator of node by last access
     */
    class NodeLastAccessComparator<G: GroupVersionedInterface<*, *>, T: NodeVersionedInterface<G>>(
        database: Database,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<G, T>(database, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val lastAccessCompare = object1.lastAccessTime.date
                    .compareTo(object2.lastAccessTime.date)
            return if (lastAccessCompare == 0)
                NodeNaturalComparator<G, T>(database, sortNodeParameters)
                        .compare(object1, object2)
            else
                lastAccessCompare
        }
    }
}
