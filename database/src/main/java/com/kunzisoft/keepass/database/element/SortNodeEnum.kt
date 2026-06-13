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

import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.GroupInfo
import com.kunzisoft.keepass.model.SortedNodeInfo

/**
 * Enumeration of available sorting methods for database nodes.
 */
enum class SortNodeEnum {
    /** Sort by natural database order. */
    DB,
    /** Sort by title alphabetically. */
    TITLE,
    /** Sort by username alphabetically. */
    USERNAME,
    /** Sort by creation time. */
    CREATION_TIME,
    /** Sort by last modification time. */
    LAST_MODIFY_TIME,
    /** Sort by last access time. */
    LAST_ACCESS_TIME;

    /**
     * Returns a comparator corresponding to the sort type.
     * @param sortNodeParameters Parameters for the sorting behavior.
     * @return A [Comparator] for [SortedNodeInfo].
     */
    fun getNodeComparator(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters,
    ) : Comparator<SortedNodeInfo> {
        return when (this) {
            DB -> NodeNaturalComparator(sortDatabaseParameters, sortNodeParameters) // Force false because natural order contains recycle bin
            TITLE -> NodeTitleComparator(sortDatabaseParameters, sortNodeParameters)
            USERNAME -> NodeUsernameComparator(sortDatabaseParameters, sortNodeParameters)
            CREATION_TIME -> NodeCreationComparator(sortDatabaseParameters, sortNodeParameters)
            LAST_MODIFY_TIME -> NodeLastModificationComparator(sortDatabaseParameters, sortNodeParameters)
            LAST_ACCESS_TIME -> NodeLastAccessComparator(sortDatabaseParameters, sortNodeParameters)
        }
    }

    /**
     * Configuration parameters for node sorting.
     * @property ascending Whether to sort in ascending order.
     * @property groupsBefore Whether groups should appear before entries.
     * @property recycleBinBottom Whether the recycle bin should be at the bottom of the list.
     */
    data class SortNodeParameters(
        var ascending: Boolean = true,
        var groupsBefore: Boolean = true,
        var recycleBinBottom: Boolean = true
    )

    /**
     * Database parameters for node sorting.
     * @param recycleBinId The id of the recycle bin.
     * @param recycleBinEnabled Whether the recycle bin is enabled.
     */
    data class SortDatabaseParameters(
        var recycleBinEnabled: Boolean = true,
        var recycleBinId: GroupId? = null
    )

    /**
     * Abstract base class for node comparators.
     * @param T The type of node info.
     * @property sortNodeParameters The sorting parameters.
     */
    abstract class NodeComparator
            <T: SortedNodeInfo>(
        var sortDatabaseParameters: SortDatabaseParameters,
        var sortNodeParameters: SortNodeParameters) : Comparator<T> {

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

            when (object1) {
                is GroupInfo -> {
                    when (object2) {
                        is GroupInfo -> {
                            // RecycleBin at end of groups
                            if (sortDatabaseParameters.recycleBinEnabled && sortNodeParameters.recycleBinBottom) {
                                if (sortDatabaseParameters.recycleBinId == object1.nodeId)
                                    return 1
                                if (sortDatabaseParameters.recycleBinId == object2.nodeId)
                                    return -1
                            }
                            return specificOrderOrHashIfEquals(object1, object2)
                        }
                        else -> {
                            return if (sortNodeParameters.groupsBefore)
                                -1
                            else
                                1
                        }
                    }
                }
                else -> {
                    return when (object2) {
                        is GroupInfo -> {
                            if (sortNodeParameters.groupsBefore)
                                1
                            else
                                -1
                        }
                        else -> {
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
    class NodeNaturalComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            return object1.indexInParent
                    .compareTo(object2.indexInParent)
        }
    }

    /**
     * Comparator of Node by Title
     */
    class NodeTitleComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val titleCompare = object1.title.compareTo(object2.title, ignoreCase = true)
            return if (titleCompare == 0)
                NodeNaturalComparator<T>(sortDatabaseParameters, sortNodeParameters)
                        .compare(object1, object2)
            else
                titleCompare
        }
    }

    /**
     * Comparator of Node by Username, Groups by title
     */
    class NodeUsernameComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            return if (object1 is EntryInfo && object2 is EntryInfo) {
                // To get username if it's a ref
                val usernameCompare = object1.username
                        .compareTo(object2.username, ignoreCase = true)
                if (usernameCompare == 0)
                    NodeTitleComparator<T>(sortDatabaseParameters, sortNodeParameters)
                            .compare(object1, object2)
                else
                    usernameCompare
            } else {
                NodeTitleComparator<T>(sortDatabaseParameters, sortNodeParameters)
                        .compare(object1, object2)
            }
        }
    }

    /**
     * Comparator of node by creation
     */
    class NodeCreationComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val creationCompare = object1.creationTime
                    .compareTo(object2.creationTime)
            return if (creationCompare == 0)
                NodeNaturalComparator<T>(sortDatabaseParameters, sortNodeParameters)
                        .compare(object1, object2)
            else
                creationCompare
        }
    }

    /**
     * Comparator of node by last modification
     */
    class NodeLastModificationComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val lastModificationCompare = object1.lastModificationTime
                    .compareTo(object2.lastModificationTime)
            return if (lastModificationCompare == 0)
                NodeNaturalComparator<T>(sortDatabaseParameters, sortNodeParameters)
                        .compare(object1, object2)
            else
                lastModificationCompare
        }
    }

    /**
     * Comparator of node by last access
     */
    class NodeLastAccessComparator<T: SortedNodeInfo>(
        sortDatabaseParameters: SortDatabaseParameters,
        sortNodeParameters: SortNodeParameters
    ) : NodeComparator<T>(sortDatabaseParameters, sortNodeParameters) {

        override fun compareBySpecificOrder(object1: T, object2: T): Int {
            val lastAccessCompare = object1.lastAccessTime
                    .compareTo(object2.lastAccessTime)
            return if (lastAccessCompare == 0)
                NodeNaturalComparator<T>(sortDatabaseParameters, sortNodeParameters)
                        .compare(object1, object2)
            else
                lastAccessCompare
        }
    }
}
