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

import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.NodeVersioned
import com.kunzisoft.keepass.database.element.Type
import java.util.*

enum class SortNodeEnum {
    DB, TITLE, USERNAME, CREATION_TIME, LAST_MODIFY_TIME, LAST_ACCESS_TIME;

    fun getNodeComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean): Comparator<NodeVersioned> {
        return when (this) {
            DB -> NodeNaturalComparator(ascending, groupsBefore, false) // Force false because natural order contains recycle bin
            TITLE -> NodeTitleComparator(ascending, groupsBefore, recycleBinBottom)
            USERNAME -> NodeUsernameComparator(ascending, groupsBefore, recycleBinBottom)
            CREATION_TIME -> NodeCreationComparator(ascending, groupsBefore, recycleBinBottom)
            LAST_MODIFY_TIME -> NodeLastModificationComparator(ascending, groupsBefore, recycleBinBottom)
            LAST_ACCESS_TIME -> NodeLastAccessComparator(ascending, groupsBefore, recycleBinBottom)
        }
    }

    abstract class NodeComparator(var ascending: Boolean, var groupsBefore: Boolean, var recycleBinBottom: Boolean) : Comparator<NodeVersioned> {

        abstract fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int

        private fun specificOrderOrHashIfEquals(object1: NodeVersioned, object2: NodeVersioned): Int {
            val specificOrderComp = compareBySpecificOrder(object1, object2)

            return if (specificOrderComp == 0) {
                object1.hashCode() - object2.hashCode()
            } else if (!ascending) -specificOrderComp else specificOrderComp // If descending, revert
        }

        override fun compare(object1: NodeVersioned,object2: NodeVersioned): Int {
            if (object1 == object2)
                return 0

            if (object1.type == Type.GROUP) {
                return if (object2.type == Type.GROUP) {
                    // RecycleBin at end of groups
                    if (recycleBinBottom) {
                        if (Database.getInstance().recycleBin == object1)
                            return 1
                        if (Database.getInstance().recycleBin == object2)
                            return -1
                    }

                    specificOrderOrHashIfEquals(object1, object2)
                } else if (object2.type == Type.ENTRY) {
                    if (groupsBefore)
                        -1
                    else
                        1
                } else {
                    -1
                }
            } else if (object1.type == Type.ENTRY) {
                return if (object2.type == Type.ENTRY) {
                    specificOrderOrHashIfEquals(object1, object2)
                } else if (object2.type == Type.GROUP) {
                    if (groupsBefore)
                        1
                    else
                        -1
                } else {
                    -1
                }
            }

            // Type not known
            return -1
        }
    }

    /**
     * Comparator of node by natural database placement
     */
    class NodeNaturalComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            return object1.nodePositionInParent.compareTo(object2.nodePositionInParent)
        }
    }

    /**
     * Comparator of Node by Title
     */
    class NodeTitleComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            return object1.title.compareTo(object2.title, ignoreCase = true)
        }
    }

    /**
     * Comparator of Node by Username, Groups by title
     */
    class NodeUsernameComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            if (object1.type == Type.ENTRY && object2.type == Type.ENTRY) {
                // To get username if it's a ref
                return (object1 as EntryVersioned).getEntryInfo(Database.getInstance()).username
                        .compareTo((object2 as EntryVersioned).getEntryInfo(Database.getInstance()).username,
                                ignoreCase = true)
            }
            return NodeTitleComparator(ascending, groupsBefore, recycleBinBottom).compare(object1, object2)
        }
    }

    /**
     * Comparator of node by creation
     */
    class NodeCreationComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            return object1.creationTime.date
                    ?.compareTo(object2.creationTime.date) ?: 0
        }
    }

    /**
     * Comparator of node by last modification
     */
    class NodeLastModificationComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            return object1.lastModificationTime.date
                    ?.compareTo(object2.lastModificationTime.date) ?: 0
        }
    }

    /**
     * Comparator of node by last access
     */
    class NodeLastAccessComparator(ascending: Boolean, groupsBefore: Boolean, recycleBinBottom: Boolean)
        : NodeComparator(ascending, groupsBefore, recycleBinBottom) {

        override fun compareBySpecificOrder(object1: NodeVersioned, object2: NodeVersioned): Int {
            return object1.lastAccessTime.date
                    ?.compareTo(object2.lastAccessTime.date) ?: 0
        }
    }
}
