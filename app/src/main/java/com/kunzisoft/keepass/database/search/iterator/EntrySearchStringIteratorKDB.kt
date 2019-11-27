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
package com.kunzisoft.keepass.database.search.iterator

import com.kunzisoft.keepass.database.element.EntryKDB
import com.kunzisoft.keepass.database.search.SearchParameters

import java.util.NoSuchElementException

class EntrySearchStringIteratorKDB

@JvmOverloads
constructor(private val mEntry: EntryKDB,
            private val mSearchParameters: SearchParameters? = SearchParameters())
    : EntrySearchStringIterator() {

    private var current = 0

    private val currentString: String
        get() {
            return when (current) {
                title -> mEntry.title
                url -> mEntry.url
                username -> mEntry.username
                comment -> mEntry.notes
                else -> ""
            }
        }

    override fun hasNext(): Boolean {
        return current < maxEntries
    }

    override fun next(): String {
        // Past the end of the list
        if (current == maxEntries) {
            throw NoSuchElementException("Past final string")
        }

        useSearchParameters()

        val str = currentString
        current++
        return str
    }

    private fun useSearchParameters() {

        if (mSearchParameters == null) {
            return
        }

        var found = false
        while (!found) {
            found = when (current) {
                title -> mSearchParameters.searchInTitles
                url -> mSearchParameters.searchInUrls
                username -> mSearchParameters.searchInUserNames
                comment -> mSearchParameters.searchInNotes
                else -> true
            }

            if (!found) {
                current++
            }
        }
    }

    companion object {

        private const val title = 0
        private const val url = 1
        private const val username = 2
        private const val comment = 3
        private const val maxEntries = 4
    }

}
