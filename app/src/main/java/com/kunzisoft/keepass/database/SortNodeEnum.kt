/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.kunzisoft.keepass.database

import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.element.NodeVersioned

import java.util.Comparator

enum class SortNodeEnum {
    DB, TITLE, USERNAME, CREATION_TIME, LAST_MODIFY_TIME, LAST_ACCESS_TIME;

    fun getNodeComparator(ascending: Boolean, groupsBefore: Boolean): Comparator<NodeVersioned> {
        return when (this) {
            DB -> NodeNaturalComparator(ascending, groupsBefore)
            TITLE -> NodeTitleComparator(ascending, groupsBefore)
            USERNAME -> NodeCreationComparator(ascending, groupsBefore) // TODO Sort
            CREATION_TIME -> NodeCreationComparator(ascending, groupsBefore)
            LAST_MODIFY_TIME -> NodeLastModificationComparator(ascending, groupsBefore)
            LAST_ACCESS_TIME -> NodeLastAccessComparator(ascending, groupsBefore)
        }
    }

    abstract class NodeComparator(internal var ascending: Boolean, private var groupsBefore: Boolean) : Comparator<NodeVersioned> {

        internal fun compareWith(comparatorGroup: Comparator<GroupVersioned>,
                                 comparatorEntry: Comparator<EntryVersioned>,
                                 object1: NodeVersioned,
                                 object2: NodeVersioned,
                                 resultOfNodeMethodCompare: Int): Int {
            if (object1 == object2)
                return 0

            if (object1 is GroupVersioned) {
                return if (object2 is GroupVersioned) {
                    comparatorGroup
                            .compare(object1, object2)
                } else if (object2 is EntryVersioned) {
                    if (groupsBefore)
                        -1
                    else
                        1
                } else {
                    -1
                }
            } else if (object1 is EntryVersioned) {
                return if (object2 is EntryVersioned) {
                    comparatorEntry
                            .compare(object1, object2)
                } else if (object2 is GroupVersioned) {
                    if (groupsBefore)
                        1
                    else
                        -1
                } else {
                    -1
                }
            }

            // If same name, can be different
            return if (resultOfNodeMethodCompare == 0) object1.hashCode() - object2.hashCode() else resultOfNodeMethodCompare
        }
    }

    /**
     * Comparator of node by natural database placement
     */
    class NodeNaturalComparator(ascending: Boolean, groupsBefore: Boolean) : NodeComparator(ascending, groupsBefore) {

        override fun compare(object1: NodeVersioned, object2: NodeVersioned): Int {

            return compareWith(
                    GroupNaturalOrderComparator(ascending),
                    EntryNaturalOrderComparator(ascending),
                    object1,
                    object2,
                    object1.creationTime.date
                            ?.compareTo(object2.creationTime.date) ?: 0)
        }
    }

    /**
     * Comparator of Node by Title, Groups first, Entries second
     */
    class NodeTitleComparator(ascending: Boolean, groupsBefore: Boolean) : NodeComparator(ascending, groupsBefore) {

        override fun compare(object1: NodeVersioned, object2: NodeVersioned): Int {

            return compareWith(
                    GroupNameComparator(ascending),
                    EntryNameComparator(ascending),
                    object1,
                    object2,
                    object1.title
                            .compareTo(object2.title, ignoreCase = true))
        }
    }

    /**
     * Comparator of node by creation, Groups first, Entries second
     */
    class NodeCreationComparator(ascending: Boolean, groupsBefore: Boolean) : NodeComparator(ascending, groupsBefore) {

        override fun compare(object1: NodeVersioned, object2: NodeVersioned): Int {

            return compareWith(
                    GroupCreationComparator(ascending),
                    EntryCreationComparator(ascending),
                    object1,
                    object2,
                    object1.creationTime.date
                            ?.compareTo(object2.creationTime.date) ?: 0)
        }
    }

    /**
     * Comparator of node by last modification, Groups first, Entries second
     */
    class NodeLastModificationComparator(ascending: Boolean, groupsBefore: Boolean) : NodeComparator(ascending, groupsBefore) {

        override fun compare(object1: NodeVersioned, object2: NodeVersioned): Int {

            return compareWith(
                    GroupLastModificationComparator(ascending),
                    EntryLastModificationComparator(ascending),
                    object1,
                    object2,
                    object1.lastModificationTime.date
                            ?.compareTo(object2.lastModificationTime.date) ?: 0)
        }
    }

    /**
     * Comparator of node by last access, Groups first, Entries second
     */
    class NodeLastAccessComparator(ascending: Boolean, groupsBefore: Boolean) : NodeComparator(ascending, groupsBefore) {

        override fun compare(object1: NodeVersioned, object2: NodeVersioned): Int {

            return compareWith(
                    GroupLastAccessComparator(ascending),
                    EntryLastAccessComparator(ascending),
                    object1,
                    object2,
                    object1.lastAccessTime.date
                            ?.compareTo(object2.lastAccessTime.date) ?: 0)
        }
    }

    abstract class AscendingComparator<Node>(private val ascending: Boolean) : Comparator<Node> {

        private fun compareWithAscending(specificOrderComp: Int): Int {
            // If descending, revert
            return if (!ascending) -specificOrderComp else specificOrderComp
        }

        abstract fun compareBySecificOrder(object1: Node, object2: Node): Int

        override fun compare(object1: Node, object2: Node): Int {
            if (object1 == object2)
                return 0

            val specificOrderComp = compareBySecificOrder(object1, object2)
            return if (specificOrderComp == 0) {
                object1.hashCode() - object2.hashCode()
            } else compareWithAscending(specificOrderComp)
        }
    }

    /**
     * Group comparator by natural order
     */
    class GroupNaturalOrderComparator(ascending: Boolean) : AscendingComparator<GroupVersioned>(ascending) {
        override fun compareBySecificOrder(object1: GroupVersioned, object2: GroupVersioned): Int {
            return object1.nodePositionInParent.compareTo(object2.nodePositionInParent)
        }
    }

    /**
     * Group comparator by name
     */
    class GroupNameComparator(ascending: Boolean) : AscendingComparator<GroupVersioned>(ascending) {
        override fun compareBySecificOrder(object1: GroupVersioned, object2: GroupVersioned): Int {
            return object1.title.compareTo(object2.title, ignoreCase = true)
        }
    }

    /**
     * Group comparator by name
     */
    class GroupCreationComparator(ascending: Boolean) : AscendingComparator<GroupVersioned>(ascending) {
        override fun compareBySecificOrder(object1: GroupVersioned, object2: GroupVersioned): Int {
            return object1.creationTime.date
                    ?.compareTo(object2.creationTime.date) ?: 0
        }
    }

    /**
     * Group comparator by last modification
     */
    class GroupLastModificationComparator(ascending: Boolean) : AscendingComparator<GroupVersioned>(ascending) {
        override fun compareBySecificOrder(object1: GroupVersioned, object2: GroupVersioned): Int {
            return object1.lastModificationTime.date
                    ?.compareTo(object2.lastModificationTime.date) ?: 0
        }
    }

    /**
     * Group comparator by last access
     */
    class GroupLastAccessComparator(ascending: Boolean) : AscendingComparator<GroupVersioned>(ascending) {
        override fun compareBySecificOrder(object1: GroupVersioned, object2: GroupVersioned): Int {
            return object1.lastAccessTime.date
                    ?.compareTo(object2.lastAccessTime.date) ?: 0
        }
    }

    /**
     * Comparator of Entry by Name
     */
    class EntryNaturalOrderComparator(ascending: Boolean) : AscendingComparator<EntryVersioned>(ascending) {
        override fun compareBySecificOrder(object1: EntryVersioned, object2: EntryVersioned): Int {
            return object1.nodePositionInParent.compareTo(object2.nodePositionInParent)
        }
    }

    /**
     * Comparator of Entry by Name
     */
    class EntryNameComparator(ascending: Boolean) : AscendingComparator<EntryVersioned>(ascending) {
        override fun compareBySecificOrder(object1: EntryVersioned, object2: EntryVersioned): Int {
            return object1.title.compareTo(object2.title, ignoreCase = true)
        }
    }

    /**
     * Comparator of Entry by Creation
     */
    class EntryCreationComparator(ascending: Boolean) : AscendingComparator<EntryVersioned>(ascending) {
        override fun compareBySecificOrder(object1: EntryVersioned, object2: EntryVersioned): Int {
            return object1.creationTime.date
                    ?.compareTo(object2.creationTime.date) ?: 0
        }
    }

    /**
     * Comparator of Entry by Last Modification
     */
    class EntryLastModificationComparator(ascending: Boolean) : AscendingComparator<EntryVersioned>(ascending) {
        override fun compareBySecificOrder(object1: EntryVersioned, object2: EntryVersioned): Int {
            return object1.lastModificationTime.date
                    ?.compareTo(object2.lastModificationTime.date) ?: 0
        }
    }

    /**
     * Comparator of Entry by Last Access
     */
    class EntryLastAccessComparator(ascending: Boolean) : AscendingComparator<EntryVersioned>(ascending) {
        override fun compareBySecificOrder(object1: EntryVersioned, object2: EntryVersioned): Int {
            return object1.lastAccessTime.date
                    ?.compareTo(object2.lastAccessTime.date) ?: 0
        }
    }
}
