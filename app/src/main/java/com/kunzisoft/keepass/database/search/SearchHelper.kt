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
package com.kunzisoft.keepass.database.search

import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIterator
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorKDB
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorKDBX

class SearchHelper(private val isOmitBackup: Boolean) {

    companion object {
        const val MAX_SEARCH_ENTRY = 6
    }

    private var incrementEntry = 0

    fun createVirtualGroupWithSearchResult(database: Database, searchQuery: String, max: Int): Group? {

        val searchGroup = database.createGroup()
        searchGroup?.title = "\"" + searchQuery + "\""

        // Search all entries
        incrementEntry = 0
        database.rootGroup?.doForEachChild(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        if (incrementEntry >= max)
                            return false
                        if (entryContainsString(node, searchQuery)) {
                            searchGroup?.addChildEntry(node)
                            incrementEntry++
                        }
                        // Stop searching when we have max entries
                        return incrementEntry < max
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        return when {
                            incrementEntry >= max -> false
                            database.isGroupSearchable(node, isOmitBackup) -> true
                            else -> false
                        }
                    }
                },
                false)

        return searchGroup
    }

    private fun entryContainsString(entry: Entry, searchString: String): Boolean {

        // Entry don't contains string if the search string is empty
        if (searchString.isEmpty())
            return false

        // Search all strings in the entry
        var iterator: EntrySearchStringIterator? = null
        entry.entryKDB?.let {
            iterator = EntrySearchStringIteratorKDB(it)
        }
        entry.entryKDBX?.let {
            iterator = EntrySearchStringIteratorKDBX(it)
        }

        iterator?.let {
            while (it.hasNext()) {
                val currentString = it.next()
                if (currentString.isNotEmpty()
                        && currentString.contains(searchString, true)) {
                        return true
                }
            }
        }
        return false
    }
}
