/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.search

import com.kunzisoft.keepass.database.NodeHandler
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.EntryVersioned
import com.kunzisoft.keepass.database.element.GroupVersioned
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIterator
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorV3
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorV4
import java.util.*

class SearchDbHelper(private val isOmitBackup: Boolean) {

    companion object {
        const val MAX_SEARCH_ENTRY = 6
    }

    private var incrementEntry = 0

    fun search(database: Database, qStr: String, max: Int): GroupVersioned? {

        val searchGroup = database.createGroup()
        searchGroup?.title = "\"" + qStr + "\""

        // Search all entries
        val loc = Locale.getDefault()
        val finalQStr = qStr.toLowerCase(loc)

        incrementEntry = 0
        database.rootGroup?.doForEachChild(
                object : NodeHandler<EntryVersioned>() {
                    override fun operate(node: EntryVersioned): Boolean {
                        if (incrementEntry >= max)
                            return false
                        if (entryContainsString(node, finalQStr, loc)) {
                            searchGroup?.addChildEntry(node)
                            incrementEntry++
                        }
                        // Stop searching when we have max entries
                        return incrementEntry < max
                    }
                },
                object : NodeHandler<GroupVersioned>() {
                    override fun operate(node: GroupVersioned): Boolean {
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

    private fun entryContainsString(entry: EntryVersioned, searchString: String, locale: Locale): Boolean {

        // Entry don't contains string if the search string is empty
        if (searchString.isEmpty())
            return false

        // Search all strings in the entry
        var iterator: EntrySearchStringIterator? = null
        entry.pwEntryV3?.let {
            iterator = EntrySearchStringIteratorV3(it)
        }
        entry.pwEntryV4?.let {
            iterator = EntrySearchStringIteratorV4(it)
        }

        iterator?.let {
            while (it.hasNext()) {
                val str = it.next()
                if (str.isNotEmpty()) {
                    if (str.toLowerCase(locale).contains(searchString)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
