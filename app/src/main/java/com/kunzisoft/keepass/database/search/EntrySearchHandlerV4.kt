/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
import com.kunzisoft.keepass.database.element.PwEntryV4
import com.kunzisoft.keepass.database.search.iterator.EntrySearchStringIteratorV4
import com.kunzisoft.keepass.utils.StringUtil

import java.util.Date
import java.util.Locale

class EntrySearchHandlerV4(private val mSearchParametersV4: SearchParametersV4, private val mListStorage: MutableList<PwEntryV4>) : NodeHandler<PwEntryV4>() {

    private var now: Date = Date()

    override fun operate(node: PwEntryV4): Boolean {
        if (mSearchParametersV4.respectEntrySearchingDisabled && !node.isSearchingEnabled) {
            return true
        }

        if (mSearchParametersV4.excludeExpired && node.isExpires && now.after(node.expiryTime.date)) {
            return true
        }

        var term = mSearchParametersV4.searchString
        if (mSearchParametersV4.ignoreCase) {
            term = term.toLowerCase()
        }

        if (searchStrings(node, term)) {
            mListStorage.add(node)
            return true
        }

        if (mSearchParametersV4.searchInGroupNames) {
            val parent = node.parent
            if (parent != null) {
                var groupName = parent.title
                if (mSearchParametersV4.ignoreCase) {
                    groupName = groupName.toLowerCase()
                }

                if (groupName.contains(term)) {
                    mListStorage.add(node)
                    return true
                }
            }
        }

        if (searchID(node)) {
            mListStorage.add(node)
            return true
        }

        return true
    }

    private fun searchID(entry: PwEntryV4): Boolean {
        if (mSearchParametersV4.searchInUUIDs) {
            val hex = UuidUtil.toHexString(entry.id)
            return StringUtil.indexOfIgnoreCase(hex, mSearchParametersV4.searchString, Locale.ENGLISH) >= 0
        }

        return false
    }

    private fun searchStrings(entry: PwEntryV4, term: String): Boolean {
        val iterator = EntrySearchStringIteratorV4(entry, mSearchParametersV4)
        while (iterator.hasNext()) {
            var str = iterator.next()
            if (str.isNotEmpty()) {
                if (mSearchParametersV4.ignoreCase) {
                    str = str.toLowerCase()
                }

                if (str.contains(term)) {
                    return true
                }
            }
        }

        return false
    }
}
